package com.elfmcys.ysm.network.protocol;

import com.elfmcys.ysm.proto.network.protocol.v0.CommonV0;
import com.elfmcys.ysm.proto.network.protocol.v0.PlayerStateV0;

import java.util.Set;

public record PlayerStateReportPolicy(Set<PlayerStateSection> requestedSections,
                                      int minDeltaIntervalMillis,
                                      int fullSnapshotIntervalMillis) {
    public PlayerStateReportPolicy {
        requestedSections = Set.copyOf(requestedSections);
        if (minDeltaIntervalMillis < 50 || minDeltaIntervalMillis > 1000) {
            throw new IllegalArgumentException("Delta interval must be between 50 and 1000 ms");
        }
        if (fullSnapshotIntervalMillis != 0
                && (fullSnapshotIntervalMillis < 5000 || fullSnapshotIntervalMillis > 300_000)) {
            throw new IllegalArgumentException("Full snapshot interval must be zero or between 5 seconds and 5 minutes");
        }
    }

    public boolean acceptsProjection(PlayerStateV0.PlayerStateReport report) {
        if (report.hasGameplay() && !requestedSections.contains(PlayerStateSection.GAMEPLAY)
                || report.hasEffects() && !requestedSections.contains(PlayerStateSection.EFFECTS)
                || report.hasAnimation() && !requestedSections.contains(PlayerStateSection.ANIMATION)
                || report.hasRoaming() && !requestedSections.contains(PlayerStateSection.ROAMING)) {
            return false;
        }
        return report.getMode() == CommonV0.StateWriteMode.STATE_WRITE_MODE_FULL
                || report.hasGameplay() || report.hasEffects()
                || report.hasAnimation() || report.hasRoaming();
    }
}
