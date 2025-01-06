package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.DebugInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class DebugAnimationScreen implements IGuiOverlay {
    private static final int DEBUG_BG_WIDTH = 1000;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (DebugAnimationKey.TYPE == DebugAnimationKey.DebugType.CUSTOM) {
            renderCustom(gui, graphics);
        }
    }

    private static void renderCustom(ForgeGui gui, GuiGraphics graphics) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
            int[] y = {5};

            DebugInfo debugInfo = cap.getDebugInfo();
            debugInfo.enumerate((name, result) -> {
                renderCustomText(gui, graphics, y, name, result);
            });
        });
    }

    private static void renderCustomText(ForgeGui gui, GuiGraphics graphics, int[] y, String name, String result) {
        Font font = gui.getFont();
        if ((y[0] - 5) % 20 == 0) {
            graphics.fill(2, y[0] - 1, DEBUG_BG_WIDTH, y[0] + 9, 0xc0505050);
        } else {
            graphics.fill(2, y[0] - 1, DEBUG_BG_WIDTH, y[0] + 9, 0xc0506050);
        }
        graphics.drawString(font, name, 5, y[0], 0xffffff);
        graphics.drawString(font, result, 260, y[0], 0xffffff);
        y[0] = y[0] + 10;
    }
}
