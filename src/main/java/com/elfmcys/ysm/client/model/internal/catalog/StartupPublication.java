package com.elfmcys.ysm.client.model.internal.catalog;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/** Stages the latest startup value and publishes it before live updates are admitted. */
final class StartupPublication<T> {
    enum Disposition {
        STAGED,
        LIVE,
        CLOSED
    }

    private State state = State.STARTING;
    private T pending;

    synchronized Disposition stage(T value) {
        Objects.requireNonNull(value, "value");
        return switch (state) {
            case STARTING -> {
                pending = value;
                yield Disposition.STAGED;
            }
            case LIVE -> Disposition.LIVE;
            case CLOSED -> Disposition.CLOSED;
        };
    }

    synchronized T promote(UnaryOperator<T> finisher, Consumer<T> publisher) {
        Objects.requireNonNull(finisher, "finisher");
        Objects.requireNonNull(publisher, "publisher");
        if (state != State.STARTING) {
            throw new IllegalStateException("Startup publication is not pending");
        }
        if (pending == null) {
            throw new IllegalStateException("Startup publication has no staged value");
        }
        var value = Objects.requireNonNull(finisher.apply(pending), "finished value");
        publisher.accept(value);
        pending = null;
        state = State.LIVE;
        return value;
    }

    synchronized boolean live() {
        return state == State.LIVE;
    }

    synchronized void close() {
        pending = null;
        state = State.CLOSED;
    }

    private enum State {
        STARTING,
        LIVE,
        CLOSED
    }
}
