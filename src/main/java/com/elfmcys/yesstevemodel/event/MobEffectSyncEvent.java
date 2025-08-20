package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.DispatchServerDrivenProperty;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
@SuppressWarnings("resource")
public class MobEffectSyncEvent {
    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        if (!YesSteveModel.isAvailable() || event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Player player) {
            var effectInstance = event.getEffectInstance();
            NetworkHandler.broadcastToVisiblePlayersAndSelf(DispatchServerDrivenProperty.addEffect(player.getId(), effectInstance.getEffect(), effectInstance.getAmplifier() + 1), player);
        }
    }

    @SubscribeEvent
    public static void onRemoved(MobEffectEvent.Remove event) {
        if (!YesSteveModel.isAvailable() || event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Player player) {
            NetworkHandler.broadcastToVisiblePlayersAndSelf(DispatchServerDrivenProperty.removeEffect(player.getId(), event.getEffect()), player);
        }
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        if (!YesSteveModel.isAvailable() || event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Player player && event.getEffectInstance() != null) {
            NetworkHandler.broadcastToVisiblePlayersAndSelf(DispatchServerDrivenProperty.removeEffect(player.getId(), event.getEffectInstance().getEffect()), player);
        }
    }
}
