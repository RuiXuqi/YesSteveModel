package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class LoginEvent {
    public static void onLoggedInServer(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (NetworkHandler.isPlayerChannelPresent(serverPlayer)) {
                ServerModelManager.syncModelsToPlayer(serverPlayer, null);
            }
        }
    }
}
