package com.elfmcys.ysm.capability;

import com.elfmcys.ysm.event.LivingShieldBlockEvent;
import com.elfmcys.ysm.network.NetworkHandler;
import com.elfmcys.ysm.network.message.DispatchServerDrivenProperty;
import com.elfmcys.ysm.util.TokenBucket;
import it.unimi.dsi.fastutil.objects.Object2ByteArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMaps;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import org.apache.commons.lang3.StringUtils;

public class ServerDrivenPlayerPropertiesTracker {
    private TokenBucket rateLimiter;
    private boolean lowBandwidth;
    private DispatchServerDrivenProperty packet;

    private int expLevel = -1;
    private boolean fly;
    private int health = -1;
    private int maxHealth = -1;
    private int foodLevel = -1;
    private float xxa = 0;
    private float yya = 0;
    private float zza = 0;
    private boolean inShieldBlockCooldown = false;
    private String extraAnimation = "";

    public ServerDrivenPlayerPropertiesTracker() {
        packet = new DispatchServerDrivenProperty(-1);
        setLowBandwidth(false);
    }

    public void setLowBandwidth(boolean value) {
        if (value != this.lowBandwidth || this.rateLimiter == null) {
            this.lowBandwidth = value;
            if (this.lowBandwidth) {
                this.rateLimiter = new TokenBucket(3, 3);
            } else {
                this.rateLimiter = new TokenBucket(4, 7);
            }
        }
    }

    private DispatchServerDrivenProperty setupPacket(ServerPlayer player, boolean sync) {
        if (!sync || packet.entityId != player.getId()) {
            packet.clear(player.getId());
        }
        return packet;
    }

    private void broadcastPacket(ServerPlayer player) {
        if (!packet.isEmpty() && rateLimiter.request()) {
            NetworkHandler.broadcastToVisiblePlayersAndSelf(packet, player);
            packet = new DispatchServerDrivenProperty(player.getId());
        }
    }

    public void tick(ServerPlayer player, boolean sync, boolean lowBandwidthUsage) {
        setLowBandwidth(lowBandwidthUsage);
        var packet = setupPacket(player, sync);

        if (expLevel != player.experienceLevel) {
            expLevel = player.experienceLevel;
            if (sync) {
                packet.expLevel(expLevel);
            }
        }
        if (fly != player.getAbilities().flying) {
            fly = player.getAbilities().flying;
            if (sync) {
                packet.flying(fly);
            }
        }
        if (health != (int) player.getHealth()) {
            health = (int) player.getHealth();
            if (sync) {
                packet.health(health);
            }
        }
        if (maxHealth != (int) player.getMaxHealth()) {
            maxHealth = (int) player.getMaxHealth();
            if (sync) {
                packet.maxHealth(maxHealth);
            }
        }
        if (foodLevel != player.getFoodData().getFoodLevel()) {
            foodLevel = player.getFoodData().getFoodLevel();
            if (sync) {
                packet.foodLevel(foodLevel);
            }
        }
        if (xxa != player.xxa) {
            xxa = player.xxa;
            if (sync) {
                packet.xxa(xxa);
            }
        }
        if (yya != player.yya) {
            yya = player.yya;
            if (sync) {
                packet.yya(yya);
            }
        }
        if (zza != player.zza) {
            zza = player.zza;
            if (sync) {
                packet.zza(zza);
            }
        }
        boolean playerCooldown = LivingShieldBlockEvent.inShieldBlockCooldown(player);
        if (this.inShieldBlockCooldown != playerCooldown) {
            this.inShieldBlockCooldown = playerCooldown;
            if (sync) {
                packet.inShieldBlockCooldown(inShieldBlockCooldown);
            }
        }

        if (sync) {
            broadcastPacket(player);
        }
    }

    public void addEffect(ServerPlayer player, MobEffect effect, int level) {
        setupPacket(player, true).addEffect(effect, level);
    }

    public void removeEffect(ServerPlayer player, MobEffect effect) {
        setupPacket(player, true).removeEffect(effect);
    }

    public void setExtraAnimation(ServerPlayer player, boolean sync, String animation) {
        if (!StringUtils.isEmpty(animation) || !StringUtils.isEmpty(extraAnimation)) {
            this.extraAnimation = animation;
            setupPacket(player, sync).extraAnimation(animation);
            if (sync) {
                broadcastPacket(player);
            }
        }
    }

    public void updateMolangVars(ServerPlayer player, boolean sync, int hashShort, Object2FloatMap<String> vars) {
        if (!lowBandwidth && sync) {
            setupPacket(player, true).molangVars(hashShort, vars);
            broadcastPacket(player);
        }
    }

    /**
     * 全量同步
     * 为避免 CME 必须在主线程上调用
     */
    public DispatchServerDrivenProperty full(ServerPlayer player, boolean broadcast) {
        if (broadcast) {
            this.packet.clear(player.getId());
        }
        var msg = new DispatchServerDrivenProperty(player.getId());
        msg.setFull();

        msg.flying(player.getAbilities().flying);
        msg.expLevel(player.experienceLevel);
        msg.foodLevel(player.getFoodData().getFoodLevel());

        var effectInstances = player.getActiveEffects();
        if (effectInstances.isEmpty()) {
            msg.allEffect(Object2ByteMaps.emptyMap());
        } else if (effectInstances.size() == 1) {
            var effectInstance = effectInstances.iterator().next();
            msg.allEffect(Object2ByteMaps.singleton(effectInstance.getEffect(), (byte) (effectInstance.getAmplifier() + 1)));
        } else {
            var effectArray = new MobEffect[effectInstances.size()];
            var levelArray = new byte[effectInstances.size()];
            var i = 0;
            for (var effectInstance : effectInstances) {
                effectArray[i] = effectInstance.getEffect();
                levelArray[i] = (byte) (effectInstance.getAmplifier() + 1);
                ++i;
            }
            msg.allEffect(new Object2ByteArrayMap<>(effectArray, levelArray));
        }
        msg.health((int) player.getHealth());
        msg.maxHealth((int) player.getMaxHealth());

        msg.xxa(player.xxa);
        msg.yya(player.yya);
        msg.zza(player.zza);

        if (LivingShieldBlockEvent.inShieldBlockCooldown(player)) {
            msg.inShieldBlockCooldown(true);
        }

        msg.extraAnimation(extraAnimation);

        return msg;
    }
}
