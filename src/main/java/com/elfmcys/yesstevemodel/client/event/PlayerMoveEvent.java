package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SetPlayAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PlayerMoveEvent {
    private static boolean LOCK_EXTRA_ANIMATION = false;

    @SubscribeEvent
    public static void onKeyboardInput(InputEvent.Key event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (isMoveKey() && player != null) {
            player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
                if (!LOCK_EXTRA_ANIMATION && cap.isPlayingAnimation()) {
                    if (NetworkHandler.isRemoteChannelPresent()) {
                        NetworkHandler.sendToServer(SetPlayAnimation.stop());
                    } else {
                        cap.stopAnimation();
                    }
                }
            });
        }
    }

    public static boolean isMoveKey() {
        Options options = Minecraft.getInstance().options;
        return options.keyUp.isDown() || options.keyDown.isDown() || options.keyLeft.isDown() || options.keyRight.isDown()
               || options.keyJump.isDown() || options.keyShift.isDown();
    }

    public static void switchLock() {
        LOCK_EXTRA_ANIMATION = !LOCK_EXTRA_ANIMATION;
    }

    public static boolean isLocked() {
        return LOCK_EXTRA_ANIMATION;
    }
}
