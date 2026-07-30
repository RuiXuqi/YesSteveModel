package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.capability.PlayerAnimatableCapability;
import com.elfmcys.ysm.event.LivingShieldBlockEvent;
import com.elfmcys.ysm.geckolib3.core.molang.util.StringPool;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.network.protocol.EntityRefEncoder;
import com.elfmcys.ysm.network.protocol.NegotiatedSessionPolicy;
import com.elfmcys.ysm.network.protocol.PlayerStateReportLifecycle;
import com.elfmcys.ysm.network.protocol.PlayerStateSection;
import com.elfmcys.ysm.network.protocol.ProtocolMessages;
import com.elfmcys.ysm.network.session.ProtocolTransport;
import com.elfmcys.ysm.proto.network.protocol.v0.CommonV0;
import com.elfmcys.ysm.proto.network.protocol.v0.PlayerStateV0;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class LocalPlayerStateReporter {
    private static final int MAX_ANIMATION_ID_BYTES = 256;
    private static final int MAX_ROAMING_VARIABLES = 64;
    private static final int MAX_VARIABLE_NAME_BYTES = 32;
    private final EntityRefEncoder entityRefs;
    private final PlayerStateReportLifecycle lifecycle = new PlayerStateReportLifecycle();
    private ProtocolTransport transport;
    private NegotiatedSessionPolicy policy;
    private boolean sendInFlight;
    private boolean authorityKnown;
    private ModelHash authoritativeModelHash;
    private Integer authoritativeRoamingKey;
    private long lastAttemptNanos;
    private long lastFullNanos;
    private GameplaySnapshot lastGameplay;
    private Map<String, Integer> lastEffects = Map.of();
    private String animationId = "";
    private String sentAnimationId;
    private int lastRoamingKey;
    private Map<String, Float> lastRoaming = Map.of();

    LocalPlayerStateReporter(EntityRefEncoder entityRefs) {
        this.entityRefs = entityRefs;
    }

    synchronized void setAnimation(String animationId) {
        this.animationId = animationId == null || animationId.isBlank()
                || animationId.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                > MAX_ANIMATION_ID_BYTES ? "" : animationId;
    }

    synchronized long awaitAuthoritativeFull(boolean clearAnimation) {
        if (!bindCurrentSession()) {
            return -1;
        }
        if (clearAnimation) {
            animationId = "";
        }
        sendInFlight = false;
        lastAttemptNanos = 0;
        return lifecycle.awaitAuthority();
    }

    synchronized void resumeAfterRequestFailure(long expectedGeneration) {
        if (expectedGeneration < 0 || !authorityKnown
                || !lifecycle.isCurrent(expectedGeneration)
                || lifecycle.phase() != PlayerStateReportLifecycle.Phase.WAIT_AUTHORITY) {
            return;
        }
        lifecycle.authorityReceived(false);
        resetSnapshots();
    }

    synchronized void acceptAuthoritativeFull(ModelHash modelHash, Integer roamingKey) {
        if (!bindCurrentSession()) {
            return;
        }
        var roamingRequested = policy.stateReportPolicy().requestedSections()
                .contains(PlayerStateSection.ROAMING);
        if (roamingRequested && roamingKey == null) {
            return;
        }
        var authorityChanged = authorityKnown
                && (!Objects.equals(authoritativeModelHash, modelHash)
                || roamingRequested && !Objects.equals(authoritativeRoamingKey, roamingKey));
        var rebind = lifecycle.phase() == PlayerStateReportLifecycle.Phase.WAIT_AUTHORITY
                || authorityChanged;
        if (authorityChanged) {
            animationId = "";
        }
        authorityKnown = true;
        authoritativeModelHash = modelHash;
        authoritativeRoamingKey = roamingKey;
        lifecycle.authorityReceived(authorityChanged);
        if (rebind) {
            sendInFlight = false;
            resetSnapshots();
        }
    }

    synchronized void tick(LocalPlayer player, PlayerAnimatableCapability capability) {
        if (!bindCurrentSession()) {
            return;
        }
        entityRefs.observePlayer(player.getId(), player.getUUID());
        if (!lifecycle.canSend() || sendInFlight) {
            return;
        }

        var now = System.nanoTime();
        var minInterval = policy.stateReportPolicy().minDeltaIntervalMillis() * 1_000_000L;
        if (lastAttemptNanos != 0 && now - lastAttemptNanos < minInterval) {
            return;
        }
        var fullInterval = policy.stateReportPolicy().fullSnapshotIntervalMillis() * 1_000_000L;
        var full = lifecycle.needsFull()
                || fullInterval != 0 && now - lastFullNanos >= fullInterval;
        var report = PlayerStateV0.PlayerStateReport.newInstance()
                .setMode(full ? CommonV0.StateWriteMode.STATE_WRITE_MODE_FULL
                        : CommonV0.StateWriteMode.STATE_WRITE_MODE_DELTA);
        var changed = full;
        var sections = policy.stateReportPolicy().requestedSections();

        var currentGameplay = lastGameplay;
        if (sections.contains(PlayerStateSection.GAMEPLAY)) {
            currentGameplay = GameplaySnapshot.capture(player);
            var gameplay = currentGameplay.toProto(full ? null : lastGameplay);
            if (full || gameplay.getSerializedSize() != 0) {
                report.setGameplay(gameplay);
                changed = true;
            }
        }

        var currentEffects = lastEffects;
        if (sections.contains(PlayerStateSection.EFFECTS)) {
            currentEffects = effects(player);
            var effects = effectDelta(currentEffects, full ? Map.of() : lastEffects, full);
            if (full || effects.getEffects().length() != 0) {
                report.setEffects(effects);
                changed = true;
            }
        }

        var currentAnimation = animationId;
        if (sections.contains(PlayerStateSection.ANIMATION)
                && (full || !currentAnimation.equals(sentAnimationId))) {
            report.setAnimation(currentAnimation.isEmpty()
                    ? PlayerStateV0.AnimationState.newInstance().setStopped(true)
                    : PlayerStateV0.AnimationState.newInstance().setAnimationId(currentAnimation));
            changed = true;
        }

        var currentRoamingKey = lastRoamingKey;
        var currentRoaming = lastRoaming;
        if (sections.contains(PlayerStateSection.ROAMING)) {
            if (authoritativeRoamingKey == null) {
                return;
            }
            var roamingSnapshot = capability.roamingSnapshot(authoritativeRoamingKey);
            currentRoamingKey = roamingSnapshot.modelKey();
            currentRoaming = roaming(roamingSnapshot.values());
            var keyChanged = currentRoamingKey != lastRoamingKey;
            var roaming = roamingDelta(currentRoamingKey, currentRoaming,
                    full || keyChanged ? Map.of() : lastRoaming);
            if (full || keyChanged || roaming.getVariables().length() != 0) {
                report.setRoaming(roaming);
                changed = true;
            }
        }
        if (!changed) {
            return;
        }

        var encoded = entityRefs.encodePlayer(player.getId());
        var sequence = lifecycle.allocateSequence();
        var generation = lifecycle.generation();
        report.setSubject(encoded.value()).setSequence(sequence);
        var snapshot = new SentSnapshot(currentGameplay, currentEffects, currentAnimation,
                currentRoamingKey, currentRoaming);
        var spec = ProtocolMessages.REGISTRY.find(PlayerStateV0.PlayerStateReport.class).orElseThrow();
        sendInFlight = true;
        lastAttemptNanos = now;
        try {
            transport.send(spec, report).whenComplete((ignored, error) ->
                    completeSend(generation, full, now, snapshot, encoded, error));
        } catch (RuntimeException error) {
            if (lifecycle.isCurrent(generation)) {
                sendInFlight = false;
            }
        }
    }

    synchronized void resetSession() {
        if (transport == null && policy == null) {
            return;
        }
        entityRefs.resetSession();
        transport = null;
        policy = null;
        lifecycle.beginSession();
        sendInFlight = false;
        authorityKnown = false;
        authoritativeModelHash = null;
        authoritativeRoamingKey = null;
        animationId = "";
        resetSnapshots();
    }

    private synchronized void completeSend(long generation, boolean full, long sentNanos,
                                           SentSnapshot snapshot,
                                           EntityRefEncoder.EncodedEntityRef encoded,
                                           Throwable error) {
        if (!lifecycle.isCurrent(generation)) {
            return;
        }
        sendInFlight = false;
        if (error != null || !lifecycle.completeSend(generation, full)) {
            return;
        }
        encoded.sentCommit().run();
        lastGameplay = snapshot.gameplay();
        lastEffects = snapshot.effects();
        sentAnimationId = snapshot.animationId();
        lastRoamingKey = snapshot.roamingKey();
        lastRoaming = snapshot.roaming();
        if (full) {
            lastFullNanos = sentNanos;
        }
    }

    private boolean bindCurrentSession() {
        var currentTransport = ClientSessionRuntime.transport().orElse(null);
        var currentPolicy = ClientSessionRuntime.policy().orElse(null);
        if (currentTransport == null || currentPolicy == null) {
            resetSession();
            return false;
        }
        if (transport == currentTransport && currentPolicy.equals(policy)) {
            return true;
        }
        if (transport != null) {
            entityRefs.resetSession();
        }
        transport = currentTransport;
        policy = currentPolicy;
        lifecycle.beginSession();
        sendInFlight = false;
        authorityKnown = false;
        authoritativeModelHash = null;
        authoritativeRoamingKey = null;
        entityRefs.setMode(policy.playerIdMode());
        resetSnapshots();
        return true;
    }

    private void resetSnapshots() {
        lastAttemptNanos = 0;
        lastFullNanos = 0;
        lastGameplay = null;
        lastEffects = Map.of();
        sentAnimationId = null;
        lastRoamingKey = 0;
        lastRoaming = Map.of();
    }

    private static Map<String, Integer> effects(LocalPlayer player) {
        var result = new HashMap<String, Integer>();
        for (var effect : player.getActiveEffects()) {
            var key = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect());
            if (key != null) result.put(key.toString(), effect.getAmplifier() + 1);
        }
        return Map.copyOf(result);
    }

    private static PlayerStateV0.EffectStateSet effectDelta(Map<String, Integer> current,
                                                             Map<String, Integer> previous,
                                                             boolean full) {
        var result = PlayerStateV0.EffectStateSet.newInstance();
        current.forEach((id, level) -> {
            if (full || !level.equals(previous.get(id))) {
                result.addEffects(PlayerStateV0.EffectState.newInstance().setEffectId(id).setLevel(level));
            }
        });
        if (!full) {
            previous.keySet().stream().filter(id -> !current.containsKey(id)).forEach(id ->
                    result.addEffects(PlayerStateV0.EffectState.newInstance().setEffectId(id).setLevel(0)));
        }
        return result;
    }

    private static Map<String, Float> roaming(Int2FloatMap values) {
        var result = new HashMap<String, Float>();
        for (var entry : values.int2FloatEntrySet()) {
            var name = StringPool.getString(entry.getIntKey());
            if (result.size() >= MAX_ROAMING_VARIABLES) {
                break;
            }
            if (name != null && !name.isBlank()
                    && name.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                    <= MAX_VARIABLE_NAME_BYTES && Float.isFinite(entry.getFloatValue())) {
                result.put(name, entry.getFloatValue());
            }
        }
        return Map.copyOf(result);
    }

    private static PlayerStateV0.RoamingState roamingDelta(int modelKey, Map<String, Float> current,
                                                            Map<String, Float> previous) {
        var result = PlayerStateV0.RoamingState.newInstance().setModelKey(modelKey);
        current.forEach((name, value) -> {
            if (!value.equals(previous.get(name))) {
                result.addVariables(CommonV0.MolangVariable.newInstance().setName(name).setValue(value));
            }
        });
        return result;
    }

    private record SentSnapshot(GameplaySnapshot gameplay, Map<String, Integer> effects,
                                String animationId, int roamingKey, Map<String, Float> roaming) {
    }

    private record GameplaySnapshot(boolean flying, int experience, int food, int health, int maxHealth,
                                    int moveX, int moveY, int moveZ, boolean shieldCooldown) {
        static GameplaySnapshot capture(LocalPlayer player) {
            return new GameplaySnapshot(player.getAbilities().flying, player.experienceLevel,
                    player.getFoodData().getFoodLevel(), (int) player.getHealth(), (int) player.getMaxHealth(),
                    quantize(player.xxa), quantize(player.yya), quantize(player.zza),
                    LivingShieldBlockEvent.inShieldBlockCooldown(player));
        }

        PlayerStateV0.GameplayState toProto(GameplaySnapshot previous) {
            var result = PlayerStateV0.GameplayState.newInstance();
            if (previous == null || flying != previous.flying) result.setFlying(flying);
            if (previous == null || experience != previous.experience) result.setExperienceLevel(experience);
            if (previous == null || food != previous.food) result.setFoodLevel(food);
            if (previous == null || health != previous.health) result.setHealth(health);
            if (previous == null || maxHealth != previous.maxHealth) result.setMaxHealth(maxHealth);
            if (previous == null || moveX != previous.moveX) result.setMoveXQ7(moveX);
            if (previous == null || moveY != previous.moveY) result.setMoveYQ7(moveY);
            if (previous == null || moveZ != previous.moveZ) result.setMoveZQ7(moveZ);
            if (previous == null || shieldCooldown != previous.shieldCooldown) result.setShieldCooldown(shieldCooldown);
            return result;
        }

        private static int quantize(float value) {
            return Math.round(java.lang.Math.max(-1f, java.lang.Math.min(1f, value)) * 127f);
        }
    }
}
