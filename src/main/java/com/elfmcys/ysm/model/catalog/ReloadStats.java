package com.elfmcys.ysm.model.catalog;

import java.time.Duration;

public record ReloadStats(int reused, int directValidated, int indexCandidates,
                          int hashProbeHits, int hashProbeMisses, int converted,
                          int rejected, long discoveredBytes, Duration discoveryTime,
                          Duration probeTime, Duration conversionTime, Duration totalTime) {
    public static ReloadStats empty() {
        return new ReloadStats(0, 0, 0, 0, 0, 0, 0, 0,
                Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO);
    }
}
