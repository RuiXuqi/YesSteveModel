package com.elfmcys.ysm.client.model.internal.render;

import java.util.Objects;
import java.util.concurrent.CancellationException;

/** Serializes one final publication against owner shutdown. */
final class FinalPublicationGate {
    private boolean closed;

    synchronized void publish(Runnable publication) {
        Objects.requireNonNull(publication, "publication");
        if (closed) {
            throw new CancellationException("The publication owner was closed");
        }
        publication.run();
    }

    synchronized boolean close() {
        if (closed) {
            return false;
        }
        closed = true;
        return true;
    }
}
