package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class NativeLibUtil {
    private static final String LIB_PATH = "/META-INF/native/";
    private static final String WINDOWS_LIB_NAME = "ysm-core.dll";
    private static final String LINUX_LIB_NAME = "libysm-core.so";
    private static final String ANDROID_LIB_NAME = "libysm-core-android.so";

    private static boolean MOBILE_PLATFORM = false;
    private static boolean AVAILABLE = false;
    private static Component UNSUPPORTED_MSG;
    private static String UNSUPPORTED_MSG_STR;

    public static void loadCoreLibrary() throws IOException {
        String libPath = System.getenv("YSM_CORE_LIB");
        if (StringUtil.isNullOrEmpty(libPath)) {
            libPath = setupLib();
            if (libPath == null) {
                return;
            }
        }
        try {
            System.load(libPath);
        } catch (Throwable e) {
            YesSteveModel.LOGGER.error("Failed to load native lib", e);
            setUnsupportedPlatformMsg("Incompatible system");
            return;
        }
        AVAILABLE = true;
    }

    private static String setupLib() throws IOException {
        byte[] libData = null;
        String libFileName = null;
        Path path = null;

        // 不要用 ArchUtils，服务端没有这个库
        boolean isX64 = SystemUtils.OS_ARCH.equals("amd64") || SystemUtils.OS_ARCH.equals("x86_64");
        boolean isAArch64 = SystemUtils.OS_ARCH.equals("aarch64");
        String modVersion = ModList.get().getModFileById(YesSteveModel.MOD_ID).getFile().getModInfos().get(0).getVersion().toString();
        if (SystemUtils.IS_OS_WINDOWS) {
            if (isX64) {
                libData = readEmbeddedFile(LIB_PATH + WINDOWS_LIB_NAME);
                libFileName = "ysm-core-" + modVersion + ".dll";
                path = Path.of(System.getProperty("java.io.tmpdir"), "ysm");
            }
        } else if (SystemUtils.IS_OS_LINUX) {
            var libcType = detectLibc();
            if (libcType == LibcType.GNU) {
                if (isX64) {
                    libData = readEmbeddedFile(LIB_PATH + LINUX_LIB_NAME);
                    libFileName = "libysm-core-" + modVersion + ".so";
                    path = Path.of(System.getProperty("user.home"), ".ysm");
                }
            } else if (libcType == LibcType.BIONIC) {
                if (isAArch64) {
                    var dir = System.getenv("MOD_ANDROID_RUNTIME");
                    if (dir != null) {
                        libData = readEmbeddedFile(LIB_PATH + ANDROID_LIB_NAME);
                        libFileName = "libysm-core.so";
                        path = Path.of(dir);
                        MOBILE_PLATFORM = true;
                    } else {
                        setUnsupportedLauncherMsg();
                    }
                }
            } else {
                setUnsupportedPlatformMsg("Linux with unsupported libc");
                return null;
            }
        } else {
            setUnsupportedPlatformMsg(SystemUtils.OS_NAME);
            return null;
        }

        if (path == null) {
            setUnsupportedPlatformMsg(null);
            return null;
        }
        if (libData == null) {
            setUnsupportedBuildMsg();
        }

        try {
            if (!Files.isDirectory(path)) {
                Files.createDirectory(path);
            }
        } catch (Throwable t) {
            path = FMLPaths.CONFIGDIR.get()
                    .resolve(YesSteveModel.MOD_ID)
                    .resolve("cache");
        }
        var libPath = path
                .resolve(libFileName)
                .toAbsolutePath()
                .normalize()
                .toString();
        writeLibData(libPath, libData);
        return libPath;
    }

    public static Component getUnsupportedMsg() {
        return UNSUPPORTED_MSG;
    }

    public static String getUnsupportedMsgStr() {
        return UNSUPPORTED_MSG_STR;
    }

    public static boolean isMobilePlatform() {
        return MOBILE_PLATFORM;
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    private static void setUnsupportedPlatformMsg(@Nullable String hint) {
        if (hint == null) {
            hint = SystemUtils.OS_NAME +  " " + SystemUtils.OS_ARCH;
        }
        UNSUPPORTED_MSG = Component.translatable("error.yes_steve_model.unsupported_platform", hint);
        UNSUPPORTED_MSG_STR = "[YSM] Current platform is unsupported：" + hint;
    }

    private static void setUnsupportedBuildMsg() {
        var hint = SystemUtils.OS_NAME +  " " + SystemUtils.OS_ARCH;
        UNSUPPORTED_MSG = Component.translatable("error.yes_steve_model.unsatisfied_build", hint);
        UNSUPPORTED_MSG_STR = "[YSM] This build does not support current platform: " + hint;
    }

    private static void setUnsupportedLauncherMsg() {
        UNSUPPORTED_MSG = Component.translatable("error.yes_steve_model.unsupported_launcher");
        UNSUPPORTED_MSG_STR = "Current launcher is unsupported";
    }

    private static void writeLibData(String libPath, byte[] libData) throws IOException {
        // 考虑多实例，需要先判断存在再写入
        File libFile = new File(libPath);
        if (libFile.exists()) {
            byte[] existingLibData = FileUtils.readFileToByteArray(libFile);
            if (!Arrays.equals(libData, existingLibData)) {
                FileUtils.writeByteArrayToFile(libFile, libData, false);
            }
        } else {
            FileUtils.writeByteArrayToFile(libFile, libData, false);
        }
    }

    private static byte @Nullable[] readEmbeddedFile(String filePath) throws IOException {
        URL url = YesSteveModel.class.getResource(filePath);
        if (url == null) {
            return null;
        }
        InputStream stream = url.openStream();
        return IOUtils.readFully(stream, stream.available());
    }

    private static LibcType detectLibc() {
        try {
            var lib = NativeLibrary.getInstance(Platform.C_LIBRARY_NAME);
            if (lib != null) {
                try {
                    if (lib.getFunction("android_set_abort_message") != null) {
                        return LibcType.BIONIC;
                    }
                } catch (Throwable ignored) {
                }
                try {
                    if (lib.getFunction("gnu_get_libc_version") != null) {
                        return LibcType.GNU;
                    }
                } catch (Throwable ignored) {
                }
                return LibcType.MUSL;
            }
        } catch (Throwable e) {
            YesSteveModel.LOGGER.error("Unable to find libc", e);
        }
        return LibcType.UNKNOWN;
    }

    private enum LibcType {
        UNKNOWN,
        GNU,
        MUSL,
        BIONIC
    }
}