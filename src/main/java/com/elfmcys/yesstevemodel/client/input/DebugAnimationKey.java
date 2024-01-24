package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;


public class DebugAnimationKey {
    public static DebugType TYPE = DebugType.NONE;

    public static final KeyMapping DEBUG_ANIMATION_KEY = new KeyMapping("key.yes_steve_model.debug_animation.desc",
            KeyConflictContext.IN_GAME, KeyModifier.ALT,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B,
            "key.category.yes_steve_model");

    public static void onKeyboardInput(InputEvent.Key event) {
        if (DEBUG_ANIMATION_KEY.isDown()) {
            switch (TYPE) {
                case NONE: TYPE = DebugType.QUERIES; break;
                case QUERIES: TYPE = DebugType.CUSTOM; break;
                case CUSTOM: TYPE = DebugType.NONE; break;
            }
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
                if(TYPE == DebugType.NONE) {
                    cap.getAnimatableModel().getDebugInfo().setEnabled(false);
                } else {
                    cap.getAnimatableModel().getDebugInfo().setEnabled(true);
                }
            });

            if (TYPE == DebugType.QUERIES) {
                Minecraft.getInstance().player.sendSystemMessage(Component.translatable("message.yes_steve_model.model.debug_animation.true"));
            } else if(TYPE == DebugType.NONE) {
                Minecraft.getInstance().player.sendSystemMessage(Component.translatable("message.yes_steve_model.model.debug_animation.false"));
            }
        }
    }

    public enum DebugType {
        QUERIES,
        CUSTOM,
        NONE
    }
}
