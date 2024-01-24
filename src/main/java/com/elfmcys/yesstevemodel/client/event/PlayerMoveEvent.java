package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SetPlayAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.InputEvent;

public class PlayerMoveEvent {
    public static void onKeyboardInput(InputEvent.Key event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (isMoveKey() && player != null) {
            player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
                if (cap.isPlayingAnimation()) {
                    NetworkHandler.CHANNEL.sendToServer(SetPlayAnimation.stop());
                }
            });
        }
    }

    private static boolean isMoveKey() {
        Options options = Minecraft.getInstance().options;
        return options.keyUp.isDown() || options.keyDown.isDown() || options.keyLeft.isDown() || options.keyRight.isDown()
                || options.keyJump.isDown() || options.keyShift.isDown();
    }
}
