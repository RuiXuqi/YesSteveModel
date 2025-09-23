package com.elfmcys.yesstevemodel.client.gui.overlay;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.config.LoadingStateScreenConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class LoadingStateScreen implements IGuiOverlay {
    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (LoadingStateScreenConfig.DISABLE_LOADING_STATE_SCREEN.get()) {
            return;
        }

        // 根据配置决定位置
        LoadingStateScreenConfig.Position position = LoadingStateScreenConfig.LOADING_STATE_POSITION.get();
        int x, y, barX, barY;
        // 渲染一个 150 长度的进度条
        int barWidth = 150;
        int barHeight = 10;
        switch (position) {
            case TOP_LEFT -> {
                x = 10;
                y = 10;
                barX = 10;
                barY = 22;
            }
            case TOP_CENTER -> {
                x = screenWidth / 2;
                y = 10;
                barX = (screenWidth - barWidth) / 2;
                barY = 22;
            }
            case TOP_RIGHT -> {
                x = screenWidth - 10;
                y = 10;
                barX = screenWidth - 10 - barWidth;
                barY = 22;
            }
            case BOTTOM_LEFT -> {
                x = 10;
                y = screenHeight - 30;
                barX = 10;
                barY = screenHeight - 8 - barHeight;
            }
            case BOTTOM_CENTER -> {
                x = screenWidth / 2;
                y = screenHeight - 85;
                barX = (screenWidth - barWidth) / 2;
                barY = screenHeight - 63 - barHeight;
            }
            case BOTTOM_RIGHT -> {
                x = screenWidth - 10;
                y = screenHeight - 30;
                barX = screenWidth - 10 - barWidth;
                barY = screenHeight - 8 - barHeight;
            }
            default -> {
                x = screenWidth / 2;
                y = 10;
                barX = (screenWidth - barWidth) / 2;
                barY = 22;
            }
        }

        // 根据当前状态渲染不同的提示
        var state = ClientModelManager.getSyncState();

        // IDLE 状态单独处理
        if (state.getType() == ClientModelManager.SyncStateType.IDLE) {
            return;
        }

        // 其他状态
        MutableComponent text = Component.translatable("gui.yes_steve_model.sync_hint.title");
        switch (state.getType()) {
            case WAITING ->
                    text.append(Component.translatable("gui.yes_steve_model.sync_hint.waiting").withStyle(ChatFormatting.AQUA));
            case LOADING ->
                    text.append(Component.translatable("gui.yes_steve_model.sync_hint.loading").withStyle(ChatFormatting.GOLD));
            case PREPARING ->
                    text.append(Component.translatable("gui.yes_steve_model.sync_hint.preparing").withStyle(ChatFormatting.LIGHT_PURPLE));
            case SYNCING -> {
                if (state.getReceived() == 0) {
                    text.append(Component.translatable("gui.yes_steve_model.sync_hint.syncing").withStyle(ChatFormatting.RED));
                } else {
                    text.append(Component.literal(String.format("%s/%s", state.getReceived(), state.getTotal())).withStyle(ChatFormatting.GREEN));
                    float progress = (float) state.getReceived() / state.getTotal();
                    guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF555555);
                    guiGraphics.fill(barX, barY, barX + (int) (barWidth * progress), barY + barHeight, 0xFF00FF00);
                }
            }
        }

        this.drawStringAtPosition(gui, guiGraphics, text, x, y, screenWidth);
    }

    private void drawStringAtPosition(ForgeGui gui, GuiGraphics guiGraphics, MutableComponent text, int x, int y, int screenWidth) {
        int textWidth = gui.getFont().width(text);
        int drawX = switch (LoadingStateScreenConfig.LOADING_STATE_POSITION.get()) {
            case TOP_LEFT, BOTTOM_LEFT -> x;
            case TOP_CENTER, BOTTOM_CENTER -> (screenWidth - textWidth) / 2;
            case TOP_RIGHT, BOTTOM_RIGHT -> x - textWidth;
        };
        guiGraphics.drawString(gui.getFont(), text, drawX, y, 0xFFFFFF);
    }
}
