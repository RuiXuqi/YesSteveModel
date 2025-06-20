package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;


@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DebugAnimationKey {
    public static DebugType TYPE = DebugType.NONE;

    public static final KeyMapping DEBUG_ANIMATION_KEY = new KeyMapping("key.yes_steve_model.debug_animation.desc",
            KeyConflictContext.IN_GAME, KeyModifier.ALT,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B,
            "key.category.yes_steve_model");

    @SubscribeEvent
    public static void onKeyboardInput(InputEvent.Key event) {
        if (DEBUG_ANIMATION_KEY.isDown()) {
            switch (TYPE) {
                case NONE:
                    TYPE = DebugType.CUSTOM;
                    break;
                case CUSTOM:
                    TYPE = DebugType.NONE;
                    break;
            }
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
                if (TYPE == DebugType.NONE) {
                    cap.getDebugInfo().setEnabled(false);
                } else {
                    cap.getDebugInfo().setEnabled(true);
                }
            });

            if (TYPE == DebugType.CUSTOM) {
                Minecraft.getInstance().player.sendSystemMessage(Component.translatable("message.yes_steve_model.model.debug_animation.true"));
            } else if (TYPE == DebugType.NONE) {
                Minecraft.getInstance().player.sendSystemMessage(Component.translatable("message.yes_steve_model.model.debug_animation.false"));
            }
        }
    }

    public enum DebugType {
        CUSTOM,
        NONE
    }
}
