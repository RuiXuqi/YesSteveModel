package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.network.message.SyncModelInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PlayerJoinWorldEvent {
    @SubscribeEvent
    public static void onPlayerJoinWorld(final net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof AbstractClientPlayer) {
            SyncModelInfo.getCache(event.getEntity().getId()).ifPresent(newCap ->
                    event.getEntity().getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
                        cap.setModelAndTexture(newCap.getModelId(), newCap.getSelectTexture());
                        if (newCap.isPlayAnimation()) {
                            cap.playAnimation(newCap.getAnimation());
                        } else {
                            cap.stopAnimation();
                        }
                    }));
        }
    }
}
