package com.elfmcys.ysm.network.protocol;

public record PeerLimits(int maxMessageBytes,
                         int maxFragmentBytes,
                         int maxInFlightTransfers,
                         long maxEncodedAssetBytes,
                         long maxDecodedAssetBytes) {
    public PeerLimits {
        if (maxMessageBytes < ProtocolLimits.MIN_FRAGMENT_DATA_BYTES + 4 * 1024
                || maxFragmentBytes < ProtocolLimits.MIN_FRAGMENT_DATA_BYTES
                || maxInFlightTransfers <= 0
                || maxEncodedAssetBytes <= 0 || maxDecodedAssetBytes <= 0) {
            throw new IllegalArgumentException("Peer protocol limits are below the unstable baseline");
        }
    }
}
