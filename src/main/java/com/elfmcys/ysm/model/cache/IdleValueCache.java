package com.elfmcys.ysm.model.cache;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Single-flight cache whose completed entries expire after an idle interval. */
public final class IdleValueCache<K, V> implements AutoCloseable {
    private final long idleNanos;
    private final ValueCloser<V> closer;
    private final ConcurrentHashMap<K, Entry<V>> entries = new ConcurrentHashMap<>();

    public IdleValueCache(Duration idle, ValueCloser<V> closer) {
        if (idle.isNegative() || idle.isZero()) {
            throw new IllegalArgumentException("Idle duration must be positive");
        }
        this.idleNanos = idle.toNanos();
        this.closer = closer;
    }

    public CompletableFuture<V> get(K key, Function<K, CompletableFuture<V>> loader) {
        var now = System.nanoTime();
        var entry = entries.compute(key, (ignored, current) -> {
            if (current != null) {
                current.lastAccessNanos = now;
                return current;
            }
            var created = new Entry<V>(loader.apply(key), now);
            created.value.whenComplete((value, error) -> {
                if (error != null) {
                    entries.remove(key, created);
                }
            });
            return created;
        });
        return entry.value;
    }

    public void cleanUp() {
        var deadline = System.nanoTime() - idleNanos;
        for (Map.Entry<K, Entry<V>> mapEntry : entries.entrySet()) {
            var entry = mapEntry.getValue();
            if (entry.lastAccessNanos < deadline && entry.value.isDone()
                    && entries.remove(mapEntry.getKey(), entry)) {
                closeCompleted(entry.value);
            }
        }
    }

    public int size() {
        return entries.size();
    }

    @Override
    public void close() {
        entries.forEach((key, entry) -> closeCompleted(entry.value));
        entries.clear();
    }

    private void closeCompleted(CompletableFuture<V> future) {
        if (future.isDone() && !future.isCompletedExceptionally()) {
            var value = future.getNow(null);
            if (value != null) {
                closer.close(value);
            }
        }
    }

    private static final class Entry<V> {
        private final CompletableFuture<V> value;
        private volatile long lastAccessNanos;

        private Entry(CompletableFuture<V> value, long lastAccessNanos) {
            this.value = value;
            this.lastAccessNanos = lastAccessNanos;
        }
    }

    @FunctionalInterface
    public interface ValueCloser<V> {
        void close(V value);
    }
}
