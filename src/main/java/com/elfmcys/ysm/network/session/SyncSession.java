package com.elfmcys.ysm.network.session;

import com.elfmcys.ysm.network.protocol.NegotiatedSessionPolicy;

import java.util.Optional;

/** A connection to exactly one player-state authority. */
public interface SyncSession extends AutoCloseable {
    ActiveSessionMode mode();

    Optional<ProtocolTransport> transport();

    Optional<NegotiatedSessionPolicy> policy();

    @Override
    void close();
}
