package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.gui.ConfigScreen;
import com.elfmcys.yesstevemodel.client.gui.DisclaimerScreen;
import com.elfmcys.yesstevemodel.client.gui.PlayerModelScreen;
import com.elfmcys.yesstevemodel.config.ClientConfig;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.util.InputCheckUtil;
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
public class PlayerModelScreenKey {
    public static final KeyMapping PLAYER_MODEL_KEY = new KeyMapping("key.yes_steve_model.player_model.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            "key.category.yes_steve_model");

    @SubscribeEvent
    public static void onKeyboardInput(InputEvent.Key event) {
        if (!InputCheckUtil.isInGame()) {
            return;
        }
        if (event.getAction() == GLFW.GLFW_PRESS && InputCheckUtil.keyIsMatch(event, PLAYER_MODEL_KEY)) {
            if (!YesSteveModel.isAvailable()) {
                YesSteveModel.sendUnavailableMessage();
                return;
            }
            if (NetworkHandler.isRemoteChannelPresent() && !ServerConfig.CAN_SWITCH_MODEL.get()) {
                Minecraft.getInstance().setScreen(new ConfigScreen(null));
                return;
            }
            if (ClientConfig.DISCLAIMER_SHOW.get()) {
                Minecraft.getInstance().setScreen(new DisclaimerScreen());
            } else {
                Minecraft.getInstance().setScreen(new PlayerModelScreen());
            }
        }
    }
}
