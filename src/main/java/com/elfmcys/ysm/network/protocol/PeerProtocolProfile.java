package com.elfmcys.ysm.network.protocol;

import java.util.Objects;
import java.util.Set;

public record PeerProtocolProfile(String protocolVersion,
                                  Set<Integer> featureIds,
                                  PeerLimits receiveLimits) {
    public PeerProtocolProfile {
        Objects.requireNonNull(protocolVersion, "protocolVersion");
        if (protocolVersion.isBlank()) {
            throw new IllegalArgumentException("Invalid peer protocol version");
        }
        featureIds = Set.copyOf(featureIds);
        Objects.requireNonNull(receiveLimits, "receiveLimits");
    }
}
