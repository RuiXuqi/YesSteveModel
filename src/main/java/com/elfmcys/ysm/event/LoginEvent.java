package com.elfmcys.ysm.event;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.network.NetworkHandler;
import com.elfmcys.ysm.network.message.ServerInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public final class LoginEvent {
    @SubscribeEvent
    public static void onLoggedInServer(PlayerEvent.PlayerLoggedInEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendToClientPlayer(new ServerInfo(), serverPlayer);
        }
    }
}
