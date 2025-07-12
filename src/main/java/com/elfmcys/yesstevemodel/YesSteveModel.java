package com.elfmcys.yesstevemodel;

import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.init.ModSounds;
import com.elfmcys.yesstevemodel.util.NativeLibUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

@Mod(YesSteveModel.MOD_ID)
public class YesSteveModel {
    public static final String MOD_ID = "yes_steve_model";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    private static boolean AVAILABLE;

    public YesSteveModel() throws IOException {
        AVAILABLE = NativeLibUtil.loadCoreLibrary();
        if (!AVAILABLE) {
            return;
        }

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GeneralConfig.init());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.init());
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModSounds.SOUNDS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendUnavailableMessage() {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(getUnavailableMessage());
        }
    }

    public static Component getUnavailableMessage() {
        return Component.translatable("error.yes_steve_model.unsupported_platform", SystemUtils.OS_NAME, SystemUtils.OS_ARCH);
    }

    public static String getUnavailableMessageString() {
        return String.format("[YSM] Current platform is unsupported: %s - %s", SystemUtils.OS_NAME, SystemUtils.OS_ARCH);
    }
}