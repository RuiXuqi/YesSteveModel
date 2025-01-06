package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.gui.AnimationRouletteScreen;
import com.elfmcys.yesstevemodel.config.DisableSwitch;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class AnimationRouletteKey {
    public static final KeyMapping ANIMATION_ROULETTE_KEY = new KeyMapping("key.yes_steve_model.animation_roulette.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "key.category.yes_steve_model");

    @SubscribeEvent
    public static void onKeyboardInput(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && ANIMATION_ROULETTE_KEY.matches(event.getKey(), event.getScanCode()) && DisableSwitch.CAN_SWITCH) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
                    String modelId = cap.getModelId();
                    var model = ClientModelManager.getModels().get(modelId);
                    if (model != null && !model.modelInfo().properties().extraAnimationOrderMap().isEmpty()) {
                        if (Minecraft.getInstance().screen == null) {
                            Minecraft.getInstance().setScreen(new AnimationRouletteScreen(modelId, model.modelInfo().properties(), cap));
                            return;
                        }
                        if (Minecraft.getInstance().screen instanceof AnimationRouletteScreen) {
                            Minecraft.getInstance().setScreen(null);
                        }
                    }
                });
            }
        }
    }
}
