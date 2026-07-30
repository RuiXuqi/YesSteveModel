package com.elfmcys.ysm.client.gui.button;

import com.elfmcys.ysm.config.LoadingStateScreenConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class PositionButton extends Button {
    public PositionButton(int x, int y) {
        super(x, y, 100, 20, Component.empty(), b -> {
        }, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

        Font font = Minecraft.getInstance().font;
        MutableComponent desc = Component.translatable("gui.yes_steve_model.config.loading_state_position");
        guiGraphics.drawString(font, desc, this.getX() + 105, this.getY() + 6, 0xFFFFFF, false);
    }

    @Override
    public Component getMessage() {
        LoadingStateScreenConfig.Position position = LoadingStateScreenConfig.LOADING_STATE_POSITION.get();
        return Component.literal(position.name());
    }

    @Override
    public void onPress() {
        LoadingStateScreenConfig.Position position = LoadingStateScreenConfig.LOADING_STATE_POSITION.get();
        LoadingStateScreenConfig.Position nextPosition = switch (position) {
            case TOP_LEFT -> LoadingStateScreenConfig.Position.TOP_CENTER;
            case TOP_CENTER -> LoadingStateScreenConfig.Position.TOP_RIGHT;
            case TOP_RIGHT -> LoadingStateScreenConfig.Position.BOTTOM_RIGHT;
            case BOTTOM_RIGHT -> LoadingStateScreenConfig.Position.BOTTOM_CENTER;
            case BOTTOM_CENTER -> LoadingStateScreenConfig.Position.BOTTOM_LEFT;
            case BOTTOM_LEFT -> LoadingStateScreenConfig.Position.TOP_LEFT;
        };
        LoadingStateScreenConfig.LOADING_STATE_POSITION.set(nextPosition);
    }
}
