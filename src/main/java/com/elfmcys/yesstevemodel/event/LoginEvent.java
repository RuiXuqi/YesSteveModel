package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SyncDisableSwitch;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public final class LoginEvent {
    @SubscribeEvent
    public static void onLoggedInServer(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (NetworkHandler.isPlayerChannelPresent(serverPlayer)) {
                ServerModelManager.syncModelsToPlayer(serverPlayer, null);
                NetworkHandler.sendToClientPlayer(new SyncDisableSwitch(ServerConfig.CAN_SWITCH_MODEL.get()), serverPlayer);
            }
        }
    }
}
