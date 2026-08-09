package com.elfmcys.ysm.client.model.internal.render;

import com.elfmcys.ysm.client.model.catalog.ModelContentVersion;
import com.elfmcys.ysm.model.domain.Hash256;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Delays and aggregates only failures confirmed by an active entity binding. */
final class ModelFailureNotificationAggregator implements AutoCloseable {
    private static final long DELAY_MILLIS = 1500;

    private final ScheduledExecutorService scheduler;
    private final Map<ModelContentVersion, PendingFailure> pending = new HashMap<>();
    private final Set<EmissionKey> emitted = new HashSet<>();
    private ScheduledFuture<?> scheduled;
    private boolean closed;

    ModelFailureNotificationAggregator(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    synchronized void report(Hash256 hash, ModelContentVersion version, Outcome outcome) {
        if (closed || emitted.contains(new EmissionKey(version, outcome))) {
            return;
        }
        pending.merge(version, new PendingFailure(hash, outcome),
                (left, right) -> left.outcome().ordinal() >= right.outcome().ordinal() ? left : right);
        if (scheduled == null) {
            scheduled = scheduler.schedule(this::flush, DELAY_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    synchronized void recovered(ModelContentVersion version) {
        pending.remove(version);
        emitted.removeIf(key -> key.contentVersion().equals(version));
    }

    private void flush() {
        final Map<ModelContentVersion, PendingFailure> batch;
        synchronized (this) {
            if (closed) {
                return;
            }
            batch = Map.copyOf(pending);
            pending.clear();
            scheduled = null;
            batch.forEach((version, failure) -> emitted.add(
                    new EmissionKey(version, failure.outcome())));
        }
        if (batch.isEmpty()) {
            return;
        }
        var worst = batch.values().stream().map(PendingFailure::outcome)
                .max(java.util.Comparator.comparingInt(Enum::ordinal)).orElseThrow();
        Minecraft.getInstance().execute(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(Component.translatable(worst.translationKey(), batch.size()));
            }
        });
    }

    @Override
    public synchronized void close() {
        closed = true;
        pending.clear();
        emitted.clear();
        if (scheduled != null) {
            scheduled.cancel(false);
            scheduled = null;
        }
    }

    enum Outcome {
        PARTIAL("message.yes_steve_model.model.failure.partial"),
        FALLBACK("message.yes_steve_model.model.failure.fallback"),
        STOPPED("message.yes_steve_model.model.failure.stopped");

        private final String translationKey;

        Outcome(String translationKey) {
            this.translationKey = translationKey;
        }

        String translationKey() {
            return translationKey;
        }
    }

    private record PendingFailure(Hash256 modelHash, Outcome outcome) {
    }

    private record EmissionKey(ModelContentVersion contentVersion, Outcome outcome) {
    }
}
