package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapability;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientTickEvent {
    private static int tickCount;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tickCount++;
        ClientModelManager.tick();

        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(PlayerAnimatableCapability::handleRoamingVarsChanges);
        }
    }

    public static int getTickCount() {
        return tickCount;
    }
}
