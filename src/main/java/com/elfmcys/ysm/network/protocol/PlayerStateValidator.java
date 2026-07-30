package com.elfmcys.ysm.network.protocol;

import com.elfmcys.ysm.proto.network.protocol.v0.CommonV0;
import com.elfmcys.ysm.proto.network.protocol.v0.PlayerStateV0;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;

/** Structural and resource-limit validation performed before a player-state packet is enqueued. */
public final class PlayerStateValidator {
    private static final int MAX_TEXTURE_BYTES = 256;
    private static final int MAX_ANIMATION_BYTES = 256;
    private static final int MAX_ROAMING_VARIABLES = 64;
    private static final int MAX_VARIABLE_NAME_BYTES = 32;
    private static final int MAX_EFFECTS = 64;
    private static final int MAX_EFFECT_ID_BYTES = 128;
    private static final int MAX_GAMEPLAY_VALUE = 1_000_000;

    private PlayerStateValidator() {
    }

    public static boolean validReport(PlayerStateV0.PlayerStateReport report) {
        return report.hasSequence() && report.getSequence() != 0
                && report.hasMode() && validMode(report.getMode())
                && validEntity(report.hasSubject() ? report.getSubject() : null)
                && (!report.hasGameplay() || validGameplay(report.getGameplay()))
                && (!report.hasEffects() || validEffects(report.getEffects()))
                && (!report.hasAnimation() || validAnimation(report.getAnimation()))
                && (!report.hasRoaming() || validRoaming(report.getRoaming()));
    }

    public static boolean validUpdate(PlayerStateV0.PlayerStateUpdate update) {
        return update.hasRevision() && update.getRevision() != 0
                && update.hasMode() && validMode(update.getMode())
                && validEntity(update.hasSubject() ? update.getSubject() : null)
                && (update.getMode() != CommonV0.StateWriteMode.STATE_WRITE_MODE_FULL || update.hasModel())
                && (!update.hasModel() || validModel(update.getModel()))
                && (!update.hasGameplay() || validGameplay(update.getGameplay()))
                && (!update.hasEffects() || validEffects(update.getEffects()))
                && (!update.hasAnimation() || validAnimation(update.getAnimation()))
                && (!update.hasRoaming() || validRoaming(update.getRoaming()));
    }

    private static boolean validMode(CommonV0.StateWriteMode mode) {
        return mode == CommonV0.StateWriteMode.STATE_WRITE_MODE_FULL
                || mode == CommonV0.StateWriteMode.STATE_WRITE_MODE_DELTA;
    }

    private static boolean validEntity(CommonV0.EntityRef entity) {
        return entity != null && entity.hasEntityId() && !entity.hasPlayerId()
                && entity.getEntityId() >= 0;
    }

    private static boolean validModel(PlayerStateV0.ModelSelectionState model) {
        if (!model.hasModel() || !ModelReferenceCodec.valid(model.getModel())
                || !model.hasTextureId() || !model.hasDisabled()) {
            return false;
        }
        return utf8Length(model.getTextureId()) <= MAX_TEXTURE_BYTES;
    }

    private static boolean validGameplay(PlayerStateV0.GameplayState state) {
        if (state.hasExperienceLevel() && !between(state.getExperienceLevel(), 0, MAX_GAMEPLAY_VALUE)
                || state.hasFoodLevel() && !between(state.getFoodLevel(), 0, 20)
                || state.hasHealth() && !between(state.getHealth(), 0, MAX_GAMEPLAY_VALUE)
                || state.hasMaxHealth() && !between(state.getMaxHealth(), 0, MAX_GAMEPLAY_VALUE)
                || state.hasMoveXQ7() && !between(state.getMoveXQ7(), -127, 127)
                || state.hasMoveYQ7() && !between(state.getMoveYQ7(), -127, 127)
                || state.hasMoveZQ7() && !between(state.getMoveZQ7(), -127, 127)) {
            return false;
        }
        return !state.hasHealth() || !state.hasMaxHealth() || state.getHealth() <= state.getMaxHealth();
    }

    private static boolean validEffects(PlayerStateV0.EffectStateSet state) {
        if (state.getEffects().length() > MAX_EFFECTS) {
            return false;
        }
        var ids = new HashSet<String>();
        for (var effect : state.getEffects()) {
            if (!effect.hasEffectId() || effect.getEffectId().isBlank()
                    || utf8Length(effect.getEffectId()) > MAX_EFFECT_ID_BYTES
                    || !effect.hasLevel() || effect.getLevel() < 0 || effect.getLevel() > 255
                    || !ids.add(effect.getEffectId())) {
                return false;
            }
        }
        return true;
    }

    private static boolean validAnimation(PlayerStateV0.AnimationState state) {
        if (state.hasStopped()) {
            return state.getStopped();
        }
        return state.hasAnimationId() && !state.getAnimationId().isBlank()
                && utf8Length(state.getAnimationId()) <= MAX_ANIMATION_BYTES;
    }

    private static boolean validRoaming(PlayerStateV0.RoamingState state) {
        if (!state.hasModelKey() || state.getVariables().length() > MAX_ROAMING_VARIABLES) {
            return false;
        }
        var names = new HashSet<String>();
        for (var variable : state.getVariables()) {
            if (!variable.hasName() || variable.getName().isBlank()
                    || utf8Length(variable.getName()) > MAX_VARIABLE_NAME_BYTES
                    || !variable.hasValue() || !Float.isFinite(variable.getValue())
                    || !names.add(variable.getName())) {
                return false;
            }
        }
        return true;
    }

    private static int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private static boolean between(int value, int minimum, int maximum) {
        return value >= minimum && value <= maximum;
    }
}
