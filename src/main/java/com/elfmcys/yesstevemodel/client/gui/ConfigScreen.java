package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.client.gui.button.ConfigCheckBox;
import com.elfmcys.yesstevemodel.client.gui.button.FlatColorButton;
import com.elfmcys.yesstevemodel.config.ExtraPlayerScreenConfig;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import org.jetbrains.annotations.Nullable;

public class ConfigScreen extends Screen {
    @Nullable
    private final PlayerModelScreen parent;

    public ConfigScreen(@Nullable PlayerModelScreen parent) {
        super(Component.literal("YSM Config GUI"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = (width - 420) / 2;
        int y = (height - 235) / 2;

        addRenderableWidget(new FlatColorButton(x + 5, y + 2, 80, 18, Component.translatable("gui.yes_steve_model.model.return"), (b) -> this.getMinecraft().setScreen(parent)));

        addRenderableWidget(new ForgeSlider(x + 5, y + 24, 320, 18, Component.translatable("gui.yes_steve_model.config.sound_volume"),
                Component.literal("%"), 0, 100, GeneralConfig.SOUND_VOLUME.get(), true) {
            @Override
            protected void applyValue() {
                GeneralConfig.SOUND_VOLUME.set(this.getValue());
            }
        });

        addRenderableWidget(new ConfigCheckBox(x + 5, y + 45, "disable_self_model", GeneralConfig.DISABLE_SELF_MODEL));
        addRenderableWidget(new ConfigCheckBox(x + 5, y + 67, "disable_other_model", GeneralConfig.DISABLE_OTHER_MODEL));
        addRenderableWidget(new ConfigCheckBox(x + 5, y + 89, "print_animation_roulette_msg", GeneralConfig.PRINT_ANIMATION_ROULETTE_MSG));
        addRenderableWidget(new ConfigCheckBox(x + 5, y + 111, "disable_self_hands", GeneralConfig.DISABLE_SELF_HANDS));
        addRenderableWidget(new ConfigCheckBox(x + 5, y + 133, "disable_player_render", ExtraPlayerScreenConfig.DISABLE_PLAYER_RENDER));
        addRenderableWidget(new ConfigCheckBox(x + 5, y + 155, "disable_arrows_model", GeneralConfig.DISABLE_ARROWS_MODEL));
        addRenderableWidget(new ConfigCheckBox(x + 5, y + 177, "use_compatibility_renderer", GeneralConfig.USE_COMPATIBILITY_RENDERER));
    }

    @Override
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(graphics);
        super.render(graphics, pMouseX, pMouseY, pPartialTick);
    }
}
