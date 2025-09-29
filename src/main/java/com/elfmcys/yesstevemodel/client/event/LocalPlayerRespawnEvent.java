package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class LocalPlayerRespawnEvent {
    @SubscribeEvent
    public static void onFire(ClientPlayerNetworkEvent.Clone event) {
        if (!YesSteveModel.isAvailable()) return;

        event.getOldPlayer().reviveCaps();
        event.getOldPlayer().getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(oldCap ->
                event.getNewPlayer().getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(newCap ->
                        newCap.copyFrom(oldCap)));
        event.getOldPlayer().invalidateCaps();
    }
}
