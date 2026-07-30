package com.elfmcys.ysm.client.texture;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Coordinates the asynchronous registrations of one repeatable logical texture. */
final class TextureLoadState {
    private long generation;
    private @Nullable CompletableFuture<Void> pending;
    private @Nullable Throwable failure;

    public synchronized @Nullable Start begin() {
        if (failure != null) {
            return null;
        }
        var previous = pending;
        pending = null;
        return new Start(++generation, previous);
    }

    public synchronized boolean attach(long candidate, CompletableFuture<Void> task) {
        Objects.requireNonNull(task, "task");
        if (!isCurrent(candidate)) {
            return false;
        }
        pending = task;
        return true;
    }

    public synchronized void complete(long candidate, CompletableFuture<Void> task) {
        if (generation == candidate && pending == task) {
            pending = null;
        }
    }

    public synchronized boolean isCurrent(long candidate) {
        return generation == candidate && failure == null;
    }

    public synchronized boolean fail(long candidate, Throwable cause) {
        Objects.requireNonNull(cause, "cause");
        if (!isCurrent(candidate)) {
            return false;
        }
        failure = cause;
        pending = null;
        return true;
    }

    public synchronized Optional<Throwable> failure() {
        return Optional.ofNullable(failure);
    }

    public synchronized @Nullable CompletableFuture<Void> deactivate() {
        ++generation;
        var task = pending;
        pending = null;
        return task;
    }

    record Start(long generation, @Nullable CompletableFuture<Void> previous) {
    }
}
