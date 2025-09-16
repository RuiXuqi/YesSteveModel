package com.elfmcys.yesstevemodel.client.gui.overlay;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class LoadingStateScreen implements IGuiOverlay {
    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        var state = ClientModelManager.getSyncState();
        if (state.getType() == ClientModelManager.SyncStateType.IDLE) {
            int removedTextureQueueSize = ClientModelManager.getRemovedTextureQueueSize();
            int newModelQueueSize = ClientModelManager.getNewModelQueueSize();

            if (removedTextureQueueSize > 0) {
                MutableComponent text = Component.translatable("gui.yes_steve_model.sync_hint.title")
                        .append(Component.translatable("gui.yes_steve_model.sync_hint.clearing", removedTextureQueueSize)
                                .withStyle(ChatFormatting.RED));
                int x = screenWidth / 2;
                int y = 10;
                guiGraphics.drawCenteredString(gui.getFont(), text, x, y, 0xFFFFFF);
            } else if (newModelQueueSize > 0) {
                int loadedModelSize = ClientModelManager.getModels().size();
                int totalModelSize = loadedModelSize + newModelQueueSize;

                MutableComponent text = Component.translatable("gui.yes_steve_model.sync_hint.title")
                        .append(Component.translatable("gui.yes_steve_model.sync_hint.loading_models", newModelQueueSize, totalModelSize)
                                .withStyle(ChatFormatting.YELLOW));
                int x = screenWidth / 2;
                int y = 10;
                guiGraphics.drawCenteredString(gui.getFont(), text, x, y, 0xFFFFFF);

                // 渲染一个 150 长度的进度条
                int barWidth = 150;
                int barHeight = 10;
                int barX = (screenWidth - barWidth) / 2;
                int barY = 22;
                float progress = (float) loadedModelSize / totalModelSize;
                guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF555555);
                guiGraphics.fill(barX, barY, barX + (int) (barWidth * progress), barY + barHeight, 0xFFFFFF00);
            }
            return;
        }

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
                    // 渲染一个 150 长度的进度条
                    int barWidth = 150;
                    int barHeight = 10;
                    int barX = (screenWidth - barWidth) / 2;
                    int barY = 22;
                    float progress = (float) state.getReceived() / state.getTotal();
                    guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF555555);
                    guiGraphics.fill(barX, barY, barX + (int) (barWidth * progress), barY + barHeight, 0xFF00FF00);
                }
            }
        }

        int x = screenWidth / 2;
        int y = 10;
        guiGraphics.drawCenteredString(gui.getFont(), text, x, y, 0xFFFFFF);
    }
}
