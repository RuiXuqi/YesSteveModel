package com.elfmcys.ysm.client.model;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class ClientModelInitialization {
    private final CompletableFuture<Void> defaultReady;
    private final CompletableFuture<Void> builtinsReady;
    private final Runnable cancelBuiltins;
    private final AtomicReference<ClientModelService.BuiltinReadiness> builtinReadiness =
            new AtomicReference<>(ClientModelService.BuiltinReadiness.LOADING);
    private final AtomicReference<Throwable> builtinFailure = new AtomicReference<>();
    private final AtomicBoolean builtinCancellationRequested = new AtomicBoolean();

    ClientModelInitialization(CompletableFuture<Void> defaultReady,
                              CompletableFuture<Void> builtinsReady,
                              Runnable cancelBuiltins) {
        this.defaultReady = defaultReady;
        this.builtinsReady = builtinsReady;
        this.cancelBuiltins = cancelBuiltins;
        builtinsReady.whenComplete((ignored, error) -> {
            if (error == null) {
                builtinReadiness.compareAndSet(
                        ClientModelService.BuiltinReadiness.LOADING,
                        ClientModelService.BuiltinReadiness.READY);
            } else {
                failBuiltins(unwrap(error));
            }
        });
    }

    void awaitDefault(long timeout, TimeUnit unit) {
        await(defaultReady, timeout, unit, "Timed out while loading the builtin default model");
    }

    void awaitBuiltins(long timeout, TimeUnit unit) {
        var readiness = builtinReadiness.get();
        if (readiness == ClientModelService.BuiltinReadiness.READY) {
            return;
        }
        if (readiness == ClientModelService.BuiltinReadiness.FAILED) {
            throw failure("Failed to initialize builtin models", builtinFailure.get());
        }

        try {
            builtinsReady.get(timeout, unit);
            builtinReadiness.compareAndSet(
                    ClientModelService.BuiltinReadiness.LOADING,
                    ClientModelService.BuiltinReadiness.READY);
        } catch (TimeoutException error) {
            failAndCancelBuiltins(error);
            throw failure("Timed out while loading builtin models", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            failAndCancelBuiltins(error);
            throw failure("Interrupted while loading builtin models", error);
        } catch (ExecutionException error) {
            var cause = unwrap(error);
            failBuiltins(cause);
            throw failure("Failed to initialize builtin models", cause);
        }
    }

    ClientModelService.BuiltinReadiness builtinReadiness() {
        return builtinReadiness.get();
    }

    Optional<Throwable> builtinFailure() {
        return Optional.ofNullable(builtinFailure.get());
    }

    private void failAndCancelBuiltins(Throwable error) {
        builtinFailure.compareAndSet(null, error);
        builtinReadiness.set(ClientModelService.BuiltinReadiness.FAILED);
        if (builtinCancellationRequested.compareAndSet(false, true)) {
            try {
                cancelBuiltins.run();
            } catch (RuntimeException cancellationError) {
                error.addSuppressed(cancellationError);
            }
        }
    }

    private void failBuiltins(Throwable error) {
        builtinFailure.compareAndSet(null, error);
        builtinReadiness.compareAndSet(
                ClientModelService.BuiltinReadiness.LOADING,
                ClientModelService.BuiltinReadiness.FAILED);
    }

    private static void await(CompletableFuture<Void> future, long timeout, TimeUnit unit,
                              String timeoutMessage) {
        try {
            future.get(timeout, unit);
        } catch (TimeoutException error) {
            throw failure(timeoutMessage, error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw failure("Interrupted while loading the builtin default model", error);
        } catch (ExecutionException error) {
            throw failure("Failed to initialize the builtin default model", unwrap(error));
        }
    }

    private static IllegalStateException failure(String message, Throwable cause) {
        return new IllegalStateException(message, cause);
    }

    private static Throwable unwrap(Throwable error) {
        var current = error;
        while ((current instanceof java.util.concurrent.CompletionException
                || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
