package com.elfmcys.ysm.network.protocol;

import com.elfmcys.ysm.proto.network.protocol.v0.HandshakeV0;

import java.util.HashSet;
import java.util.Set;

import us.hebi.quickbuf.RepeatedInt;

public final class ProtocolProfiles {
    private ProtocolProfiles() {
    }

    public static PeerProtocolProfile fromClientHello(HandshakeV0.ClientHello hello) {
        return new PeerProtocolProfile(hello.getProtocolVersion(),
                featureIds(hello.getSupportedFeatureIds()), limits(hello.getReceiveLimits()));
    }

    public static PeerProtocolProfile fromServerHello(HandshakeV0.ServerHello hello) {
        return new PeerProtocolProfile(hello.getProtocolVersion(),
                featureIds(hello.getEnabledFeatureIds()), limits(hello.getReceiveLimits()));
    }

    private static Set<Integer> featureIds(RepeatedInt values) {
        var result = new HashSet<Integer>();
        for (var i = 0; i < values.length(); i++) {
            result.add(values.get(i));
        }
        return result;
    }

    private static PeerLimits limits(HandshakeV0.ProtocolLimits limits) {
        return new PeerLimits(limits.getMaxMessageBytes(), limits.getMaxFragmentBytes(),
                limits.getMaxInFlightTransfers(), limits.getMaxEncodedAssetBytes(),
                limits.getMaxDecodedAssetBytes());
    }
}
