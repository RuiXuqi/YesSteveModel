package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapability;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.mixin.client.MinecraftAccessor;
import com.elfmcys.yesstevemodel.mixin.client.TimerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class LocalPlayerTickEvent {
    private static float YAW_SPEED;
    private static float LAST_YAW;
    private static long LAST_TIME;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent tick) {
        if (tick.side != LogicalSide.CLIENT || tick.phase != TickEvent.Phase.END || !(tick.player instanceof LocalPlayer player)) {
            return;
        }

        updateYawSpeed(player);
        player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(PlayerAnimatableCapability::handlePlayerStateChanges);
    }

    private static void updateYawSpeed(LocalPlayer player) {
        long time = ((TimerAccessor) ((MinecraftAccessor) Minecraft.getInstance()).getTimer()).getLastMs();
        if (time == LAST_TIME) {
            return;
        }

        float yaw = player.getYRot();
        if (LAST_TIME > 0) {
            YAW_SPEED = (yaw - LAST_YAW) * 1000 / (time - LAST_TIME);
        }

        LAST_TIME = time;
        LAST_YAW = yaw;
    }

    public static float getYawSpeed() {
        return YAW_SPEED;
    }
}
