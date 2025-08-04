package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import net.minecraft.util.StringUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

public final class NativeLibUtil {
    private static final String LIB_PATH = "/META-INF/native/";
    private static final String WINDOWS_LIB_NAME = "ysm-core.dll";
    private static final String LINUX_LIB_NAME = "libysm-core.so";

    private static String UNSUPPORTED_PLATFORM_NAME = "";

    public static boolean loadCoreLibrary() throws IOException {
        String libPath = System.getenv("YSM_CORE_LIB");
        if (StringUtil.isNullOrEmpty(libPath)) {
            libPath = setupLib();
            if (libPath == null) {
                return false;
            }
        }
        System.load(libPath);
        return true;
    }

    private static String setupLib() throws IOException {
        byte[] libData;
        String libFileName;

        // 不要用 ArchUtils，服务端没有这个库
        boolean isX64 = SystemUtils.OS_ARCH.equals("amd64") || SystemUtils.OS_ARCH.equals("x86_64");
        String modVersion = ModList.get().getModFileById(YesSteveModel.MOD_ID).getFile().getModInfos().get(0).getVersion().toString();
        if (SystemUtils.IS_OS_WINDOWS) {
            if (!isX64) {
                UNSUPPORTED_PLATFORM_NAME = String.format("Windows %s", SystemUtils.OS_ARCH);
                return null;
            }

            libData = readEmbeddedFile(LIB_PATH + WINDOWS_LIB_NAME);
            libFileName = "ysm-core-" + modVersion + ".dll";
        } else if (SystemUtils.IS_OS_LINUX) {
            if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) {
                UNSUPPORTED_PLATFORM_NAME = "Linux (MC Client)";
                return null;
            }
            if (!isX64) {
                UNSUPPORTED_PLATFORM_NAME = String.format("Linux %s", SystemUtils.OS_ARCH);
                return null;
            }
            if (!isGLibc()) {
                UNSUPPORTED_PLATFORM_NAME = "Linux (not based on gnu libc)";
                return null;
            }

            libData = readEmbeddedFile(LIB_PATH + LINUX_LIB_NAME);
            libFileName = "libysm-core-" + modVersion + ".so";
        } else {
            UNSUPPORTED_PLATFORM_NAME = SystemUtils.OS_NAME;
            return null;
        }

        var libPath = FMLPaths.CONFIGDIR.get()
                .resolve(YesSteveModel.MOD_ID)
                .resolve("cache")
                .resolve(libFileName)
                .toAbsolutePath().toString();
        writeLibData(libPath, libData);
        return libPath;
    }

    public static String getUnsupportedPlatformName() {
        return UNSUPPORTED_PLATFORM_NAME;
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

    private static byte[] readEmbeddedFile(String filePath) throws IOException {
        URL url = YesSteveModel.class.getResource(filePath);
        if (url == null) {
            throw new IOException("Embedded file not found: " + filePath);
        }
        InputStream stream = url.openStream();
        return IOUtils.readFully(stream, stream.available());
    }

    private static boolean isGLibc() {
        try {
            var lib = NativeLibrary.getInstance(Platform.C_LIBRARY_NAME);
            if (lib == null) {
                return false;
            }
            return lib.getFunction("gnu_get_libc_version") != null;
        } catch (Throwable e) {
            return false;
        }
    }
}