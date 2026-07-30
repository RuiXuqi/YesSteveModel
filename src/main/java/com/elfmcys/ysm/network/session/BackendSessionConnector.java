package com.elfmcys.ysm.network.session;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Future backend transport adapter. The protocol/session layer does not know how it is transported. */
@FunctionalInterface
public interface BackendSessionConnector {
    CompletionStage<Optional<SyncSession>> connect();
}
