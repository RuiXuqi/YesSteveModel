package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.ServerInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public final class LoginEvent {
    @SubscribeEvent
    public static void onLoggedInServer(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendToClientPlayer(new ServerInfo(), serverPlayer);
        }
    }
}
