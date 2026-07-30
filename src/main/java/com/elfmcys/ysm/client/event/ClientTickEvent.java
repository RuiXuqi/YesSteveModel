package com.elfmcys.ysm.client.event;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.capability.PlayerAnimatableCapability;
import com.elfmcys.ysm.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.ysm.client.ClientModelManager;
import com.elfmcys.ysm.client.sound.decoder.DecoderManager;
import com.elfmcys.ysm.client.texture.CustomTextureManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientTickEvent {
    private static int tickCount;
    private static int refreshRate = 60;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        if (event.phase == TickEvent.Phase.END) {
            return;
        }
        tickCount++;
        CustomTextureManager.tick();
        ClientModelManager.tick();
        DecoderManager.tick();
        refreshRate = Minecraft.getInstance().getWindow().getRefreshRate();

        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(PlayerAnimatableCapability::handleRoamingVarsChanges);
        }
    }

    public static int getTickCount() {
        return tickCount;
    }

    public static int getRefreshRate() {
        return refreshRate;
    }
}
