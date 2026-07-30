package com.elfmcys.ysm.network.protocol;

import com.elfmcys.ysm.proto.network.protocol.v0.CommonV0;
import com.elfmcys.ysm.proto.network.protocol.v0.PlayerStateV0;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStateValidatorTest {
    @Test
    void acceptsBoundedFullReport() {
        var report = baseReport()
                .setGameplay(PlayerStateV0.GameplayState.newInstance()
                        .setHealth(20).setMaxHealth(20).setFoodLevel(20))
                .setAnimation(PlayerStateV0.AnimationState.newInstance().setStopped(true))
                .setRoaming(PlayerStateV0.RoamingState.newInstance().setModelKey(0).addVariables(
                        CommonV0.MolangVariable.newInstance().setName("speed").setValue(1.0f)));

        assertTrue(PlayerStateValidator.validReport(report));
    }

    @Test
    void rejectsUnboundedOrSemanticallyInvalidValues() {
        assertFalse(PlayerStateValidator.validReport(baseReport().setGameplay(
                PlayerStateV0.GameplayState.newInstance().setHealth(21).setMaxHealth(20))));
        assertFalse(PlayerStateValidator.validReport(baseReport().setAnimation(
                PlayerStateV0.AnimationState.newInstance().setAnimationId(" "))));
        assertFalse(PlayerStateValidator.validReport(baseReport().setRoaming(
                PlayerStateV0.RoamingState.newInstance().setModelKey(0).addVariables(
                        CommonV0.MolangVariable.newInstance().setName("value").setValue(Float.NaN)))));

        var effects = PlayerStateV0.EffectStateSet.newInstance()
                .addEffects(PlayerStateV0.EffectState.newInstance().setEffectId("minecraft:speed").setLevel(1))
                .addEffects(PlayerStateV0.EffectState.newInstance().setEffectId("minecraft:speed").setLevel(2));
        assertFalse(PlayerStateValidator.validReport(baseReport().setEffects(effects)));
    }

    @Test
    void fullUpdateRequiresModelAndNeverAcceptsPlayerIdFromWire() {
        var update = PlayerStateV0.PlayerStateUpdate.newInstance()
                .setRevision(1)
                .setMode(CommonV0.StateWriteMode.STATE_WRITE_MODE_FULL)
                .setSubject(CommonV0.EntityRef.newInstance().setEntityId(1));
        assertFalse(PlayerStateValidator.validUpdate(update));

        update.setModel(PlayerStateV0.ModelSelectionState.newInstance()
                .setModel(CommonV0.ModelReference.newInstance().setBuiltinDefault(true))
                .setTextureId("").setDisabled(false));
        assertTrue(PlayerStateValidator.validUpdate(update));
        update.getMutableSubject().setPlayerId(new byte[PlayerId.SIZE]);
        assertFalse(PlayerStateValidator.validUpdate(update));
    }

    private static PlayerStateV0.PlayerStateReport baseReport() {
        return PlayerStateV0.PlayerStateReport.newInstance()
                .setSequence(1)
                .setSubject(CommonV0.EntityRef.newInstance().setEntityId(1))
                .setMode(CommonV0.StateWriteMode.STATE_WRITE_MODE_FULL);
    }
}
