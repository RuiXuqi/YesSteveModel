package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.molang.CustomMolangParser;
import com.elfmcys.yesstevemodel.client.model.CustomPlayerModel;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.parser.ParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.widget.ForgeSlider;

import java.text.DecimalFormat;

public class FlatSlider extends ForgeSlider {
    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation(YesSteveModel.MOD_ID, "texture/roulette.png");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private final CustomPlayerModel customPlayerModel;
    private final String molang;

    public FlatSlider(int x, int y, Component prefix, double currentValue, CustomPlayerModel customPlayerModel, String molang,
                      double step, double min, double max) {
        super(x, y, 115, 15, prefix, Component.empty(), min, max, currentValue, step, 0, true);
        this.customPlayerModel = customPlayerModel;
        this.molang = molang;
    }

    @Override
    protected void applyValue() {
        try {
            IValue parsed = CustomMolangParser.parseSingleExpressionUnsafe(molang + "=" + getValue());
            this.customPlayerModel.execute(parsed, null);
        } catch (ParseException exception) {
            YesSteveModel.LOGGER.error(exception);
        }
    }

    @Override
    public String getValueString() {
        return DECIMAL_FORMAT.format(this.getValue());
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        final Minecraft mc = Minecraft.getInstance();
        guiGraphics.blitWithBorder(BUTTON_TEXTURE, this.getX(), this.getY(), 0, getTextureY() + 24, this.width, this.height, 200, 15, 2, 3, 2, 2);
        guiGraphics.blitWithBorder(BUTTON_TEXTURE, this.getX() + (int) (this.value * (double) (this.width - 8)), this.getY(), 0, getHandleTextureY() + 24, 8, this.height, 200, 15, 2, 3, 2, 2);
        renderScrollingString(guiGraphics, mc.font, 2, getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24);
    }
}
