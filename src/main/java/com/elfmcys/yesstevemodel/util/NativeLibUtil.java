package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.minecraft.util.StringUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;

public final class NativeLibUtil {
    private static final String LIB_PATH = "/META-INF/native/";
    private static final String WINDOWS_LIB_NAME = "ysm-core.dll";
    private static final String LINUX_LIB_NAME = "libysm-core.so";

    public static void loadCoreLibrary() throws IOException {
        String libPath = System.getenv("YSM_CORE_LIB");
        if (StringUtil.isNullOrEmpty(libPath)) {
            libPath = setupLib();
        }
        System.load(libPath);
    }

    private static String setupLib() throws IOException {
        byte[] libData;
        String libPath;

        Path libDir = FMLPaths.CONFIGDIR.get().resolve(YesSteveModel.MOD_ID).resolve("cache");
        String modVersion = ModList.get().getModFileById(YesSteveModel.MOD_ID).getFile().getModInfos().get(0).getVersion().toString();

        if (SystemUtils.IS_OS_WINDOWS) {
            // 不要用 ArchUtils，服务端没有这个库
            if (!SystemUtils.OS_ARCH.equals("amd64") && !SystemUtils.OS_ARCH.equals("x86_64")) {
                throw new NotImplementedException("Only Windows-x64 is supported");
            }

            libPath = libDir.resolve("ysm-core-" + modVersion + ".dll").toString();
            libData = readEmbeddedFile(LIB_PATH + WINDOWS_LIB_NAME);
        } else if (SystemUtils.IS_OS_LINUX) {
            if (!SystemUtils.OS_ARCH.equals("amd64") && !SystemUtils.OS_ARCH.equals("x86_64")) {
                throw new NotImplementedException("Only Linux-x64 is supported");
            }

            if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
                libPath = libDir.resolve("libysm-core-" + modVersion + ".so").toString();
                libData = readEmbeddedFile(LIB_PATH + LINUX_LIB_NAME);
            } else {
                throw new NotImplementedException("YSM client is not supported on linux.");
            }
        } else {
            throw new NotImplementedException(SystemUtils.OS_NAME + " is not supported");
        }

        writeLibData(libPath, libData);
        return libPath;
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
}