package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.config.ClientConfig;
import com.elfmcys.yesstevemodel.util.NativeLibUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 为了兼容 Android 平台运行 Java 版 MC
 * <p>
 * 设计此类用于一些 Android 特有的兼容性处理
 */
public class AndroidCompat {
    public static boolean isAndroid() {
        return YesSteveModel.isMobilePlatform();
    }

    @Nullable
    public static Button addYsmSkinButton(PauseScreen screen) {
        if (isAndroid()) {
            Component name = Component.translatable("gui.yes_steve_model.skin");
            return Button.builder(name, button -> {
                if (ClientConfig.DISCLAIMER_SHOW.get()) {
                    Minecraft.getInstance().setScreen(new DisclaimerScreen());
                } else {
                    Minecraft.getInstance().setScreen(new PlayerModelScreen());
                }
            }).bounds(screen.width / 2 - 100, screen.height - 30, 200, 20).build();
        }
        return null;
    }
}
