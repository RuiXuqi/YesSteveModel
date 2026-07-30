package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.network.NetworkHandler;
import com.elfmcys.ysm.network.NetworkPayload;
import com.elfmcys.ysm.network.protocol.MessageDirection;
import com.elfmcys.ysm.network.protocol.PeerProtocolProfile;
import com.elfmcys.ysm.network.protocol.ProtocolMessageSpec;
import com.elfmcys.ysm.network.session.ProtocolTransport;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

final class ForgeClientProtocolTransport implements ProtocolTransport {
    private final PeerProtocolProfile peerProfile;

    ForgeClientProtocolTransport(PeerProtocolProfile peerProfile) {
        this.peerProfile = Objects.requireNonNull(peerProfile, "peerProfile");
    }

    @Override
    public PeerProtocolProfile peerProfile() {
        return peerProfile;
    }

    @Override
    public CompletionStage<Void> send(ProtocolMessageSpec<?> spec, NetworkPayload<?> payload) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(payload, "payload");
        if (spec.direction() != MessageDirection.CLIENT_TO_SERVER
                || !spec.messageType().isInstance(payload.protobuf())) {
            payload.close();
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid client message binding"));
        }
        if (!NetworkHandler.isRemoteChannelPresent()) {
            payload.close();
            return CompletableFuture.failedFuture(new IllegalStateException("Game-server protocol channel is unavailable"));
        }
        NetworkHandler.sendToServer(payload);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() {
    }
}
