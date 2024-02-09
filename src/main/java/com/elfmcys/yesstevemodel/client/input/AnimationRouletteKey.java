package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.client.gui.AnimationRouletteScreen;
import com.elfmcys.yesstevemodel.config.DisableSwitch;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public class AnimationRouletteKey {
    public static final KeyMapping ANIMATION_ROULETTE_KEY = new KeyMapping("key.yes_steve_model.animation_roulette.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "key.category.yes_steve_model");

    public static void onKeyboardInput(InputEvent.Key event) {
        if (ANIMATION_ROULETTE_KEY.isDown() && DisableSwitch.CAN_SWITCH) {
            Minecraft.getInstance().setScreen(new AnimationRouletteScreen());
        }
    }
}
