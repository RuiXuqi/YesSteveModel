package com.elfmcys.ysm.network.protocol;

import java.util.Objects;

public record NegotiatedSessionPolicy(PlayerStateReportPolicy stateReportPolicy,
                                      PlayerIdTransmissionMode playerIdMode) {
    public NegotiatedSessionPolicy {
        Objects.requireNonNull(stateReportPolicy, "stateReportPolicy");
        Objects.requireNonNull(playerIdMode, "playerIdMode");
    }
}
