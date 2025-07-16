package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.DispatchServerDrivenProperty;
import net.minecraft.server.level.ServerPlayer;

public class ServerDrivenPlayerPropertiesTracker {
    private int expLevel = -1;
    private boolean fly;
    private int health = -1;
    private int maxHealth = -1;
    private int foodLevel = -1;

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
        if (health != player.getHealth()) {
            health = (int) player.getHealth();
            if (sync) {
                NetworkHandler.broadcastToVisiblePlayersAndSelf(DispatchServerDrivenProperty.health(player.getId(), health), player);
            }
        }
        if (maxHealth != player.getMaxHealth()) {
            maxHealth = (int) player.getMaxHealth();
            if (sync) {
                NetworkHandler.broadcastToVisiblePlayersAndSelf(DispatchServerDrivenProperty.maxHealth(player.getId(), maxHealth), player);
            }
        }
        if (foodLevel != player.getFoodData().getFoodLevel()) {
            foodLevel = player.getFoodData().getFoodLevel();
            if (sync) {
                NetworkHandler.broadcastToVisiblePlayersAndSelf(DispatchServerDrivenProperty.foodLevel(player.getId(), foodLevel), player);
            }
        }
    }
}
