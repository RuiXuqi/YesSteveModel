package com.elfmcys.ysm.client.model;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientModelInitializationTest {
    @Test
    void defaultReadinessDoesNotWaitForOptionalBuiltins() {
        var builtins = new CompletableFuture<Void>();
        var initialization = new ClientModelInitialization(
                CompletableFuture.completedFuture(null), builtins, () -> {
                });

        assertDoesNotThrow(() -> initialization.awaitDefault(1, TimeUnit.SECONDS));
        assertEquals(ClientModelService.BuiltinReadiness.LOADING,
                initialization.builtinReadiness());
    }

    @Test
    void defaultFailureIsPropagated() {
        var cause = new IllegalArgumentException("broken default");
        var initialization = new ClientModelInitialization(
                CompletableFuture.failedFuture(cause),
                new CompletableFuture<>(), () -> {
                });

        var error = assertThrows(IllegalStateException.class,
                () -> initialization.awaitDefault(1, TimeUnit.SECONDS));

        assertSame(cause, error.getCause());
    }

    @Test
    void builtinCompletionMakesPhaseReady() {
        var builtins = new CompletableFuture<Void>();
        var initialization = new ClientModelInitialization(
                CompletableFuture.completedFuture(null), builtins, () -> {
                });

        builtins.complete(null);
        initialization.awaitBuiltins(1, TimeUnit.SECONDS);

        assertEquals(ClientModelService.BuiltinReadiness.READY,
                initialization.builtinReadiness());
    }

    @Test
    void builtinFailureIsTerminalAndObservable() {
        var cause = new AssertionError("fatal builtin failure");
        var builtins = new CompletableFuture<Void>();
        var initialization = new ClientModelInitialization(
                CompletableFuture.completedFuture(null), builtins, () -> {
                });

        builtins.completeExceptionally(cause);
        var error = assertThrows(IllegalStateException.class,
                () -> initialization.awaitBuiltins(1, TimeUnit.SECONDS));

        assertSame(cause, error.getCause());
        assertEquals(ClientModelService.BuiltinReadiness.FAILED,
                initialization.builtinReadiness());
        assertSame(cause, initialization.builtinFailure().orElseThrow());
    }

    @Test
    void timeoutCancelsOnceAndLateCompletionCannotRestoreReadiness() {
        var cancellationCount = new AtomicInteger();
        var builtins = new CompletableFuture<Void>();
        var initialization = new ClientModelInitialization(
                CompletableFuture.completedFuture(null), builtins,
                cancellationCount::incrementAndGet);

        assertThrows(IllegalStateException.class,
                () -> initialization.awaitBuiltins(0, TimeUnit.MILLISECONDS));
        assertThrows(IllegalStateException.class,
                () -> initialization.awaitBuiltins(0, TimeUnit.MILLISECONDS));
        builtins.complete(null);

        assertEquals(1, cancellationCount.get());
        assertEquals(ClientModelService.BuiltinReadiness.FAILED,
                initialization.builtinReadiness());
    }
}
