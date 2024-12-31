package com.elfmcys.yesstevemodel.client.gui.button;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FlatRatioBox extends AbstractWidget {
    private final int maxHeight;

    public FlatRatioBox(int pX, int pY, int maxHeight, Component component) {
        super(pX, pY, 115, 15, component);
        this.maxHeight = maxHeight;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + maxHeight, 0xef_434242);
        this.renderScrollingString(graphics, Minecraft.getInstance().font, 2, 0xFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
