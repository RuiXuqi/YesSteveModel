package com.elfmcys.ysm.capability;

import com.elfmcys.ysm.event.LivingShieldBlockEvent;
import com.elfmcys.ysm.network.forge.PlayerStateHandler;
import com.elfmcys.ysm.proto.network.protocol.v0.CommonV0;
import com.elfmcys.ysm.proto.network.protocol.v0.PlayerStateV0;
import com.elfmcys.ysm.util.TokenBucket;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import org.apache.commons.lang3.StringUtils;
import org.joml.Math;

public final class ServerDrivenPlayerPropertiesTracker {
    private TokenBucket rateLimiter;
    private boolean lowBandwidth;
    private PlayerStateV0.GameplayState pendingGameplay = PlayerStateV0.GameplayState.newInstance();
    private final Object2IntOpenHashMap<MobEffect> pendingEffects = new Object2IntOpenHashMap<>();
    private PlayerStateV0.AnimationState pendingAnimation;
    private int pendingRoamingKey;
    private final Object2FloatOpenHashMap<String> pendingRoaming = new Object2FloatOpenHashMap<>();

    private int expLevel = -1;
    private boolean fly;
    private int health = -1;
    private int maxHealth = -1;
    private int foodLevel = -1;
    private float xxa;
    private float yya;
    private float zza;
    private boolean inShieldBlockCooldown;
    private String extraAnimation = "";

    public ServerDrivenPlayerPropertiesTracker() {
        setLowBandwidth(false);
    }

    public void setLowBandwidth(boolean value) {
        if (value != lowBandwidth || rateLimiter == null) {
            lowBandwidth = value;
            rateLimiter = value ? new TokenBucket(3, 3) : new TokenBucket(4, 7);
        }
    }

    public void tick(ServerPlayer player, boolean sync, boolean lowBandwidthUsage) {
        setLowBandwidth(lowBandwidthUsage);
        if (!sync) {
            clearPending();
        }
        if (expLevel != player.experienceLevel) {
            expLevel = player.experienceLevel;
            if (sync) pendingGameplay.setExperienceLevel(expLevel);
        }
        if (fly != player.getAbilities().flying) {
            fly = player.getAbilities().flying;
            if (sync) pendingGameplay.setFlying(fly);
        }
        if (health != (int) player.getHealth()) {
            health = (int) player.getHealth();
            if (sync) pendingGameplay.setHealth(health);
        }
        if (maxHealth != (int) player.getMaxHealth()) {
            maxHealth = (int) player.getMaxHealth();
            if (sync) pendingGameplay.setMaxHealth(maxHealth);
        }
        if (foodLevel != player.getFoodData().getFoodLevel()) {
            foodLevel = player.getFoodData().getFoodLevel();
            if (sync) pendingGameplay.setFoodLevel(foodLevel);
        }
        if (xxa != player.xxa) {
            xxa = player.xxa;
            if (sync) pendingGameplay.setMoveXQ7(quantizeAxis(xxa));
        }
        if (yya != player.yya) {
            yya = player.yya;
            if (sync) pendingGameplay.setMoveYQ7(quantizeAxis(yya));
        }
        if (zza != player.zza) {
            zza = player.zza;
            if (sync) pendingGameplay.setMoveZQ7(quantizeAxis(zza));
        }
        var cooldown = LivingShieldBlockEvent.inShieldBlockCooldown(player);
        if (inShieldBlockCooldown != cooldown) {
            inShieldBlockCooldown = cooldown;
            if (sync) pendingGameplay.setShieldCooldown(cooldown);
        }
        if (sync) {
            broadcastPending(player);
        }
    }

    public void addEffect(ServerPlayer player, MobEffect effect, int level) {
        pendingEffects.put(effect, level);
        broadcastPending(player);
    }

    public void removeEffect(ServerPlayer player, MobEffect effect) {
        pendingEffects.put(effect, 0);
        broadcastPending(player);
    }

    public void setExtraAnimation(ServerPlayer player, boolean sync, String animation) {
        if (StringUtils.isEmpty(animation) && StringUtils.isEmpty(extraAnimation)) {
            return;
        }
        extraAnimation = animation;
        pendingAnimation = animation.isEmpty()
                ? PlayerStateV0.AnimationState.newInstance().setStopped(true)
                : PlayerStateV0.AnimationState.newInstance().setAnimationId(animation);
        if (sync) {
            broadcastPending(player);
        }
    }

    public void acceptClientAnimation(String animation) {
        extraAnimation = animation;
        pendingAnimation = null;
    }

    public void updateMolangVars(ServerPlayer player, boolean sync, int modelKey,
                                 Object2FloatMap<String> variables) {
        if (lowBandwidth || !sync) {
            return;
        }
        if (pendingRoamingKey != modelKey) {
            pendingRoamingKey = modelKey;
            pendingRoaming.clear();
        }
        pendingRoaming.putAll(variables);
        broadcastPending(player);
    }

    public void acceptClientRoaming() {
        pendingRoaming.clear();
        pendingRoamingKey = 0;
    }

    public void populateFull(PlayerStateV0.PlayerStateUpdate update, ServerPlayer player) {
        var gameplay = PlayerStateV0.GameplayState.newInstance()
                .setFlying(player.getAbilities().flying)
                .setExperienceLevel(player.experienceLevel)
                .setFoodLevel(player.getFoodData().getFoodLevel())
                .setHealth((int) player.getHealth())
                .setMaxHealth((int) player.getMaxHealth())
                .setMoveXQ7(quantizeAxis(player.xxa))
                .setMoveYQ7(quantizeAxis(player.yya))
                .setMoveZQ7(quantizeAxis(player.zza))
                .setShieldCooldown(LivingShieldBlockEvent.inShieldBlockCooldown(player));
        update.setGameplay(gameplay);

        var effects = PlayerStateV0.EffectStateSet.newInstance();
        for (var instance : player.getActiveEffects()) {
            var key = BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect());
            if (key != null) {
                effects.addEffects(PlayerStateV0.EffectState.newInstance()
                        .setEffectId(key.toString())
                        .setLevel(instance.getAmplifier() + 1));
            }
        }
        update.setEffects(effects);
        update.setAnimation(extraAnimation.isEmpty()
                ? PlayerStateV0.AnimationState.newInstance().setStopped(true)
                : PlayerStateV0.AnimationState.newInstance().setAnimationId(extraAnimation));
    }

    private void broadcastPending(ServerPlayer player) {
        if (!hasPending() || !rateLimiter.request()) {
            return;
        }
        var update = PlayerStateHandler.newDelta(player);
        if (pendingGameplay.getSerializedSize() != 0) {
            update.setGameplay(pendingGameplay);
        }
        if (!pendingEffects.isEmpty()) {
            var effects = PlayerStateV0.EffectStateSet.newInstance();
            pendingEffects.object2IntEntrySet().fastForEach(entry -> {
                var key = BuiltInRegistries.MOB_EFFECT.getKey(entry.getKey());
                if (key != null) {
                    effects.addEffects(PlayerStateV0.EffectState.newInstance()
                            .setEffectId(key.toString())
                            .setLevel(entry.getIntValue()));
                }
            });
            update.setEffects(effects);
        }
        if (pendingAnimation != null) {
            update.setAnimation(pendingAnimation);
        }
        if (!pendingRoaming.isEmpty()) {
            var roaming = PlayerStateV0.RoamingState.newInstance().setModelKey(pendingRoamingKey);
            pendingRoaming.object2FloatEntrySet().fastForEach(entry -> roaming.addVariables(
                    CommonV0.MolangVariable.newInstance()
                            .setName(entry.getKey())
                            .setValue(entry.getFloatValue())));
            update.setRoaming(roaming);
        }
        PlayerStateHandler.broadcast(player, update);
        clearPending();
    }

    private boolean hasPending() {
        return pendingGameplay.getSerializedSize() != 0 || !pendingEffects.isEmpty()
                || pendingAnimation != null || !pendingRoaming.isEmpty();
    }

    private void clearPending() {
        pendingGameplay = PlayerStateV0.GameplayState.newInstance();
        pendingEffects.clear();
        pendingAnimation = null;
        pendingRoaming.clear();
        pendingRoamingKey = 0;
    }

    private static int quantizeAxis(float value) {
        return Math.round(Math.clamp(value, -1f, 1f) * 127f);
    }
}
