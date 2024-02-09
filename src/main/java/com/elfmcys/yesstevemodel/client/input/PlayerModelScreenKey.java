package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.client.gui.DisclaimerScreen;
import com.elfmcys.yesstevemodel.client.gui.PlayerModelScreen;
import com.elfmcys.yesstevemodel.config.DisableSwitch;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public class PlayerModelScreenKey {
    public static final KeyMapping PLAYER_MODEL_KEY = new KeyMapping("key.yes_steve_model.player_model.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            "key.category.yes_steve_model");

    public static void onKeyboardInput(InputEvent.Key event) {
        if (PLAYER_MODEL_KEY.isDown() && DisableSwitch.CAN_SWITCH) {
            if (GeneralConfig.DISCLAIMER_SHOW.get()) {
                Minecraft.getInstance().setScreen(new DisclaimerScreen());
            } else {
                Minecraft.getInstance().setScreen(new PlayerModelScreen());
            }
        }
    }
}
