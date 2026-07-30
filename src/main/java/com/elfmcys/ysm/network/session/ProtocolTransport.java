package com.elfmcys.ysm.network.session;

import com.elfmcys.ysm.network.NetworkPayload;
import com.elfmcys.ysm.network.protocol.PeerProtocolProfile;
import com.elfmcys.ysm.network.protocol.ProtocolMessageSpec;
import us.hebi.quickbuf.ProtoMessage;

import java.util.concurrent.CompletionStage;

public interface ProtocolTransport extends AutoCloseable {
    PeerProtocolProfile peerProfile();

    CompletionStage<Void> send(ProtocolMessageSpec<?> spec, NetworkPayload<?> payload);

    @SuppressWarnings({"rawtypes", "unchecked"})
    default CompletionStage<Void> send(ProtocolMessageSpec<?> spec, ProtoMessage<?> message) {
        return send(spec, NetworkPayload.protobuf((ProtoMessage) message));
    }

    @Override
    void close();
}
