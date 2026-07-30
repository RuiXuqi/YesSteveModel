package com.elfmcys.ysm.network.protocol;

import com.elfmcys.ysm.proto.network.protocol.v0.CommonV0;
import com.elfmcys.ysm.proto.network.protocol.v0.PlayerStateV0;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStateReportPolicyTest {
    private final PlayerStateReportPolicy policy = new PlayerStateReportPolicy(
            Set.of(PlayerStateSection.ANIMATION), 50, 30_000);

    @Test
    void acceptsOnlyNegotiatedSections() {
        var animation = report(CommonV0.StateWriteMode.STATE_WRITE_MODE_DELTA)
                .setAnimation(PlayerStateV0.AnimationState.newInstance().setStopped(true));
        var roaming = report(CommonV0.StateWriteMode.STATE_WRITE_MODE_DELTA)
                .setRoaming(PlayerStateV0.RoamingState.newInstance().setModelKey(1));

        assertTrue(policy.acceptsProjection(animation));
        assertFalse(policy.acceptsProjection(roaming));
    }

    @Test
    void fullMayOmitARequestedSectionButDeltaMayNotBeEmpty() {
        assertTrue(policy.acceptsProjection(
                report(CommonV0.StateWriteMode.STATE_WRITE_MODE_FULL)));
        assertFalse(policy.acceptsProjection(
                report(CommonV0.StateWriteMode.STATE_WRITE_MODE_DELTA)));
    }

    private static PlayerStateV0.PlayerStateReport report(CommonV0.StateWriteMode mode) {
        return PlayerStateV0.PlayerStateReport.newInstance().setMode(mode);
    }
}
