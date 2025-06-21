package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapability;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class LocalPlayerTickEvent {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent tick) {
        if (tick.side != LogicalSide.CLIENT || tick.phase != TickEvent.Phase.END || !(tick.player instanceof LocalPlayer player)) {
            return;
        }

        player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(PlayerAnimatableCapability::handleRoamingVarsChanges);
    }
}
