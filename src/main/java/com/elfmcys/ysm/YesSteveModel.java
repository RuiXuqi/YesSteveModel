package com.elfmcys.ysm;

import com.elfmcys.ysm.config.ClientConfig;
import com.elfmcys.ysm.config.ServerConfig;
import com.elfmcys.ysm.init.ModSounds;
import com.elfmcys.ysm.util.Keep;
import com.elfmcys.ysm.util.NativeLibUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModLoadingWarning;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

@Mod(YesSteveModel.MOD_ID)
@SuppressWarnings("removal")
public class YesSteveModel {
    public static final String MOD_ID = "ysm";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public YesSteveModel() throws IOException {
        NativeLibUtil.loadCoreLibrary();
        if (!NativeLibUtil.isAvailable()) {
            LOGGER.error(getUnavailableMessageString());
            return;
        }

        initConfig();
    }

    private static void initConfig() {
        var deprecatedFile = FMLPaths.CONFIGDIR.get().resolve(MOD_ID + "-common.toml").toFile();
        if (deprecatedFile.isFile()) {
            var newFile = FMLPaths.CONFIGDIR.get().resolve(MOD_ID + "-client.toml").toFile();
            if (!newFile.isFile()) {
                deprecatedFile.renameTo(newFile);
            } else {
                deprecatedFile.delete();
            }
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.init());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.init());
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModSounds.SOUNDS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    @Keep
    public static boolean isAvailable() {
        return NativeLibUtil.isAvailable();
    }

    public static boolean isMobilePlatform() {
        return NativeLibUtil.isMobilePlatform();
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendUnavailableMessage() {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(getUnavailableMessage());
        }
    }

    public static ModLoadingWarning getUnavailableWarning() {
        return NativeLibUtil.getUnavailableWarning();
    }

    public static Component getUnavailableMessage() {
        return NativeLibUtil.getUnsupportedMsg();
    }

    public static String getUnavailableMessageString() {
        return NativeLibUtil.getUnsupportedMsgStr();
    }
}