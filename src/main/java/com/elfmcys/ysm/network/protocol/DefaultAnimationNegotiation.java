package com.elfmcys.ysm.network.protocol;

import com.elfmcys.ysm.model.catalog.DefaultAnimationKey;
import com.elfmcys.ysm.proto.network.protocol.v0.HandshakeV0;

import java.util.LinkedHashSet;
import java.util.Set;

/** Strict full-name-set negotiation for the intrinsic default animation contract. */
public final class DefaultAnimationNegotiation {
    private DefaultAnimationNegotiation() {
    }

    public static Set<DefaultAnimationKey> missing(
            Iterable<HandshakeV0.AnimationName> required,
            Set<DefaultAnimationKey> available) {
        var missing = new LinkedHashSet<DefaultAnimationKey>();
        for (var key : readCanonical(required)) {
            if (!available.contains(key)) {
                missing.add(key);
            }
        }
        return Set.copyOf(missing);
    }

    public static Set<DefaultAnimationKey> missing(
            Set<DefaultAnimationKey> required,
            Iterable<HandshakeV0.AnimationName> available) {
        var missing = new LinkedHashSet<>(required);
        missing.removeAll(readCanonical(available));
        return Set.copyOf(missing);
    }

    private static Set<DefaultAnimationKey> readCanonical(
            Iterable<HandshakeV0.AnimationName> values) {
        var result = new LinkedHashSet<DefaultAnimationKey>();
        DefaultAnimationKey previous = null;
        for (var value : values) {
            final DefaultAnimationKey key;
            try {
                key = new DefaultAnimationKey(value.getDomain(), value.getName());
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("Invalid animation name in handshake", error);
            }
            if (previous != null && previous.compareTo(key) >= 0) {
                throw new IllegalArgumentException(
                        "Handshake animation names are not strictly sorted");
            }
            previous = key;
            result.add(key);
        }
        return Set.copyOf(result);
    }
}
