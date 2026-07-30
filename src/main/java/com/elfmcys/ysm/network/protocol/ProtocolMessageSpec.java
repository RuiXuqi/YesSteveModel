package com.elfmcys.ysm.network.protocol;

import java.util.Objects;

public record ProtocolMessageSpec<T>(int id,
                                     MessageDirection direction,
                                     Class<T> messageType,
                                     int maxEncodedBytes) {
    public ProtocolMessageSpec {
        if (id < 0) {
            throw new IllegalArgumentException("Protocol message id must not be negative");
        }
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(messageType, "messageType");
        if (maxEncodedBytes <= 0) {
            throw new IllegalArgumentException("Maximum encoded size must be positive");
        }
    }
}
