package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SetPlayAnimation;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ExtraAnimationKey {
    public static final List<KeyMapping> EXTRA_ANIMATION_KEYS = Lists.newArrayList();

    public static void registerKeyBinding(RegisterKeyMappingsEvent event) {
        for (int i = 0; i <= 7; i++) {
            String name = String.format("key.yes_steve_model.extra_animation.%d.desc", i);
            KeyMapping keyMapping = new KeyMapping(name,
                    KeyConflictContext.IN_GAME,
                    KeyModifier.NONE,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    "key.category.yes_steve_model");
            event.register(keyMapping);
            EXTRA_ANIMATION_KEYS.add(keyMapping);
        }
    }

    public static void onKeyboardInput(InputEvent.Key event) {
        for (KeyMapping key : EXTRA_ANIMATION_KEYS) {
            if (key.isDown()) {
                NetworkHandler.CHANNEL.sendToServer(new SetPlayAnimation(EXTRA_ANIMATION_KEYS.indexOf(key)));
                return;
            }
        }
    }
}
