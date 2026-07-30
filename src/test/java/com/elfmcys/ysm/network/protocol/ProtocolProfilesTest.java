package com.elfmcys.ysm.network.protocol;

import com.elfmcys.ysm.proto.network.protocol.v0.HandshakeV0;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolProfilesTest {
    @Test
    void acceptsAbsentFeatureListsAsEmptySets() {
        var limits = HandshakeV0.ProtocolLimits.newInstance()
                .setMaxMessageBytes(8192)
                .setMaxFragmentBytes(1024)
                .setMaxInFlightTransfers(4)
                .setMaxEncodedAssetBytes(4096)
                .setMaxDecodedAssetBytes(8192);
        var clientProfile = ProtocolProfiles.fromClientHello(HandshakeV0.ClientHello.newInstance()
                .setProtocolVersion(ProtocolVersion.CURRENT)
                .setReceiveLimits(limits));
        var serverProfile = ProtocolProfiles.fromServerHello(HandshakeV0.ServerHello.newInstance()
                .setProtocolVersion(ProtocolVersion.CURRENT)
                .setReceiveLimits(limits));

        assertTrue(clientProfile.featureIds().isEmpty());
        assertTrue(serverProfile.featureIds().isEmpty());
    }

    @Test
    void preservesNegotiatedFeaturesAndPeerLimits() {
        var hello = HandshakeV0.ServerHello.newInstance()
                .setProtocolVersion(ProtocolVersion.CURRENT)
                .addEnabledFeatureIds(7)
                .setReceiveLimits(HandshakeV0.ProtocolLimits.newInstance()
                        .setMaxMessageBytes(8192)
                        .setMaxFragmentBytes(1024)
                        .setMaxInFlightTransfers(4)
                        .setMaxEncodedAssetBytes(4096)
                        .setMaxDecodedAssetBytes(8192));

        var profile = ProtocolProfiles.fromServerHello(hello);

        assertEquals(ProtocolVersion.CURRENT, profile.protocolVersion());
        assertTrue(profile.featureIds().contains(7));
        assertEquals(1024, profile.receiveLimits().maxFragmentBytes());
    }

    @Test
    void requiresReleaseCreditFeatureAndExactCurrentVersion() {
        var limits = new PeerLimits(8192, 1024, 4, 4096, 8192);

        assertTrue(ProtocolVersion.supportsRequiredFeatures(new PeerProtocolProfile(
                ProtocolVersion.CURRENT,
                java.util.Set.of(ProtocolVersion.ASSET_TRANSFER_RELEASE_FEATURE_ID), limits)));
        assertFalse(ProtocolVersion.supportsRequiredFeatures(new PeerProtocolProfile(
                "0.1.1-unstable",
                java.util.Set.of(ProtocolVersion.ASSET_TRANSFER_RELEASE_FEATURE_ID), limits)));
        assertFalse(ProtocolVersion.supportsRequiredFeatures(new PeerProtocolProfile(
                ProtocolVersion.CURRENT, java.util.Set.of(), limits)));
    }
}
