package com.elfmcys.ysm.model.cache;

import com.elfmcys.ysm.task.TaskContext;
import com.elfmcys.ysm.task.TaskScope;
import com.elfmcys.ysm.util.Closeable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Single-flight cache whose pending entries are retained only while they have subscribers. */
public final class ScopedIdleValueCache<K, V> implements AutoCloseable {
    private final long idleNanos;
    private final IdleValueCache.ValueCloser<V> closer;
    private final ConcurrentHashMap<K, Entry> entries = new ConcurrentHashMap<>();

    public ScopedIdleValueCache(Duration idle, IdleValueCache.ValueCloser<V> closer) {
        if (idle.isNegative() || idle.isZero()) {
            throw new IllegalArgumentException("Idle duration must be positive");
        }
        this.idleNanos = idle.toNanos();
        this.closer = closer;
    }

    public CompletableFuture<V> get(TaskContext context, K key, Loader<K, V> loader) {
        if (context.cancelled()) {
            return cancelledFuture();
        }
        while (true) {
            var now = System.nanoTime();
            var entry = entries.compute(key, (ignored, current) -> {
                if (current != null && !current.retired) {
                    current.lastAccessNanos = now;
                    return current;
                }
                return new Entry(key, context, loader, now);
            });
            entry.start();
            var subscription = entry.subscribe(context);
            if (subscription != null) {
                return subscription;
            }
        }
    }

    public void cleanUp() {
        var deadline = System.nanoTime() - idleNanos;
        for (Map.Entry<K, Entry> mapEntry : entries.entrySet()) {
            var entry = mapEntry.getValue();
            if (entry.lastAccessNanos < deadline && entry.value.isDone()
                    && entries.remove(mapEntry.getKey(), entry)) {
                entry.retire(false);
            }
        }
    }

    public int size() {
        return entries.size();
    }

    @Override
    public void close() {
        var copy = new ArrayList<>(entries.values());
        entries.clear();
        for (var entry : copy) {
            entry.retire(true);
        }
    }

    private static <T> CompletableFuture<T> cancelledFuture() {
        return CompletableFuture.failedFuture(new CancellationException("Request scope was closed"));
    }

    private final class Entry {
        private final K key;
        private final Object lock = new Object();
        private final TaskScope loaderScope;
        private final CompletableFuture<V> value;
        private final HashSet<Subscription> subscriptions = new HashSet<>();
        private volatile long lastAccessNanos;
        private boolean retired;
        private boolean valueClosed;
        private boolean started;

        private Entry(K key, TaskContext context, Loader<K, V> loader, long now) {
            this.key = key;
            this.lastAccessNanos = now;
            loaderScope = TaskScope.create(context.executor(), context.immediate());
            CompletableFuture<V> created;
            try {
                created = loader.load(loaderScope, key);
            } catch (Throwable error) {
                created = CompletableFuture.failedFuture(error);
            }
            value = created;
        }

        private void start() {
            synchronized (lock) {
                if (started) {
                    return;
                }
                started = true;
            }
            value.whenComplete(this::finish);
        }

        private void finish(V result, Throwable error) {
            loaderScope.close();
            if (error != null) {
                entries.remove(key, this);
                return;
            }
            var close = false;
            synchronized (lock) {
                if (retired && !valueClosed) {
                    valueClosed = true;
                    close = true;
                }
            }
            if (close && result != null) {
                closer.close(result);
            }
        }

        private CompletableFuture<V> subscribe(TaskContext context) {
            var subscription = new Subscription(this);
            synchronized (lock) {
                if (retired) {
                    return null;
                }
                subscriptions.add(subscription);
            }
            var guard = context.guard(subscription);
            subscription.setGuard(guard);
            value.whenComplete(subscription::complete);
            return subscription.result;
        }

        private void remove(Subscription subscription) {
            var cancel = false;
            synchronized (lock) {
                subscriptions.remove(subscription);
                if (subscriptions.isEmpty() && !value.isDone() && !retired
                        && entries.remove(key, this)) {
                    retired = true;
                    cancel = true;
                }
            }
            if (cancel) {
                loaderScope.close();
            }
        }

        private void retire(boolean cancelSubscribers) {
            List<Subscription> pending = List.of();
            V completed = null;
            synchronized (lock) {
                if (retired && (!cancelSubscribers || subscriptions.isEmpty())) {
                    return;
                }
                retired = true;
                if (cancelSubscribers) {
                    pending = new ArrayList<>(subscriptions);
                }
                if (!valueClosed && value.isDone() && !value.isCompletedExceptionally()) {
                    completed = value.getNow(null);
                    valueClosed = true;
                }
            }
            loaderScope.close();
            for (var subscription : pending) {
                subscription.close();
            }
            if (completed != null) {
                closer.close(completed);
            }
        }
    }

    private final class Subscription implements AutoCloseable {
        private final Entry entry;
        private final CompletableFuture<V> result = new CompletableFuture<>();
        private final AtomicBoolean done = new AtomicBoolean();
        private volatile Closeable guard;

        private Subscription(Entry entry) {
            this.entry = entry;
        }

        private void setGuard(Closeable guard) {
            this.guard = guard;
            if (done.get() && guard != null) {
                guard.close();
            }
        }

        private void complete(V value, Throwable error) {
            if (!done.compareAndSet(false, true)) {
                return;
            }
            entry.remove(this);
            var currentGuard = guard;
            if (currentGuard != null) {
                currentGuard.close();
            }
            if (error == null) {
                result.complete(value);
            } else {
                result.completeExceptionally(error);
            }
        }

        @Override
        public void close() {
            if (!done.compareAndSet(false, true)) {
                return;
            }
            entry.remove(this);
            var currentGuard = guard;
            if (currentGuard != null) {
                currentGuard.close();
            }
            result.completeExceptionally(new CancellationException("Request scope was closed"));
        }
    }

    @FunctionalInterface
    public interface Loader<K, V> {
        CompletableFuture<V> load(TaskContext context, K key);
    }
}
