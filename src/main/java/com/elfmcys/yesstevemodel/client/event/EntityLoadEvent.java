package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.network.message.SyncArrowModelInfo;
import com.elfmcys.yesstevemodel.network.message.SyncModelInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class EntityLoadEvent {
    @SubscribeEvent
    public static void onEntityLoadToWorld(final net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof AbstractClientPlayer) {
            SyncModelInfo.recoverFromCache(event.getEntity());
        } else if (event.getEntity() instanceof AbstractArrow) {
            SyncArrowModelInfo.recoverFromCache((AbstractArrow) event.getEntity());
        }
    }
}
