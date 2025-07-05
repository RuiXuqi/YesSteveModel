package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.DispatchServerDrivenProperty;
import net.minecraft.server.level.ServerPlayer;

public class ServerDrivenPlayerPropertiesTracker {
    private int expLevel = -1;
    private boolean fly;

    public void tick(ServerPlayer player, boolean sync) {
        if (expLevel != player.experienceLevel) {
            expLevel = player.experienceLevel;
            if (sync) {
                NetworkHandler.broadcastToVisiblePlayersAndSelf(DispatchServerDrivenProperty.expLevel(player.getId(), expLevel), player);
            }
        }
        if (fly != player.getAbilities().flying) {
            fly = player.getAbilities().flying;
            if (sync) {
                NetworkHandler.broadcastToVisiblePlayersAndSelf(DispatchServerDrivenProperty.flying(player.getId(), fly), player);
            }
        }
    }
}
