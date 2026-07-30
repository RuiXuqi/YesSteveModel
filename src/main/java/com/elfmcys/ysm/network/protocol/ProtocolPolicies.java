package com.elfmcys.ysm.network.protocol;

import com.elfmcys.ysm.proto.network.protocol.v0.HandshakeV0;

import java.util.EnumSet;

public final class ProtocolPolicies {
    private ProtocolPolicies() {
    }

    public static NegotiatedSessionPolicy fromServerHello(HandshakeV0.ServerHello hello) {
        if (!hello.hasStateReportPolicy() || !hello.hasEntityRefPolicy()) {
            throw new IllegalArgumentException("Server hello is missing required session policies");
        }
        var sections = EnumSet.noneOf(PlayerStateSection.class);
        for (var value : hello.getStateReportPolicy().getRequestedSections()) {
            sections.add(switch (value) {
                case PLAYER_STATE_SECTION_GAMEPLAY -> PlayerStateSection.GAMEPLAY;
                case PLAYER_STATE_SECTION_EFFECTS -> PlayerStateSection.EFFECTS;
                case PLAYER_STATE_SECTION_ANIMATION -> PlayerStateSection.ANIMATION;
                case PLAYER_STATE_SECTION_ROAMING -> PlayerStateSection.ROAMING;
                default -> throw new IllegalArgumentException("Unsupported player state section: " + value);
            });
        }
        var reportPolicy = new PlayerStateReportPolicy(sections,
                hello.getStateReportPolicy().getMinDeltaIntervalMs(),
                hello.getStateReportPolicy().getFullSnapshotIntervalMs());
        var playerIdMode = switch (hello.getEntityRefPolicy().getPlayerIdMode()) {
            case PLAYER_ID_MODE_FORBIDDEN -> PlayerIdTransmissionMode.FORBIDDEN;
            case PLAYER_ID_MODE_ON_MAPPING_CHANGE -> PlayerIdTransmissionMode.ON_MAPPING_CHANGE;
            default -> throw new IllegalArgumentException("Unsupported player id policy");
        };
        return new NegotiatedSessionPolicy(reportPolicy, playerIdMode);
    }
}
