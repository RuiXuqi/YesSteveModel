package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.DispatchServerDrivenProperty;
import it.unimi.dsi.fastutil.objects.Object2ByteArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMaps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

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

    /**
     * 全量同步
     * 为避免 CME 必须在主线程上调用
     */
    public static DispatchServerDrivenProperty full(ServerPlayer player) {
        var msg = new DispatchServerDrivenProperty(player.getId(), 0);

        msg.flying = player.getAbilities().flying;
        msg.expLevel = player.experienceLevel;
        msg.foodLevel = player.getFoodData().getFoodLevel();

        var effectInstances = player.getActiveEffects();
        if (effectInstances.isEmpty()) {
            msg.effects = Object2ByteMaps.emptyMap();
        } else if (effectInstances.size() == 1) {
            var effectInstance = effectInstances.iterator().next();
            msg.effects = Object2ByteMaps.singleton(effectInstance.getEffect(), (byte) (effectInstance.getAmplifier() + 1));
        } else {
            var effectArray = new MobEffect[effectInstances.size()];
            var levelArray = new byte[effectInstances.size()];
            var i = 0;
            for (var effectInstance : effectInstances) {
                effectArray[i] = effectInstance.getEffect();
                levelArray[i] = (byte) (effectInstance.getAmplifier() + 1);
                ++i;
            }
            msg.effects = new Object2ByteArrayMap<>(effectArray, levelArray);
        }
        msg.health = (int) player.getHealth();
        msg.maxHealth = (int) player.getMaxHealth();

        return msg;
    }
}
