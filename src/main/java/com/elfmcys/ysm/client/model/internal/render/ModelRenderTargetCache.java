package com.elfmcys.ysm.client.model.internal.render;

import com.elfmcys.ysm.client.model.ModelRenderTarget;
import com.elfmcys.ysm.client.model.ModelRenderTargetLease;
import com.elfmcys.ysm.client.model.catalog.ModelContentVersion;
import com.elfmcys.ysm.task.TaskContext;
import com.elfmcys.ysm.task.TaskScope;
import com.elfmcys.ysm.util.Closeable;
import net.minecraft.client.Minecraft;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ModelRenderTargetCache implements AutoCloseable {
    private static final long IDLE_NANOS = Duration.ofSeconds(30).toNanos();

    private final Map<ModelRenderTargetRequestKey, Entry> entries = new ConcurrentHashMap<>();
    private final ModelFailureRegistry failures;

    public ModelRenderTargetCache(ModelFailureRegistry failures) {
        this.failures = failures;
    }

    public CompletableFuture<ModelRenderTargetLease> acquire(
            TaskContext context, ModelRenderTargetRequestKey key, Retention retention, Loader loader) {
        if (context.cancelled()) {
            return cancelledFuture();
        }
        var frozen = failures.findTarget(key).orElse(null);
        if (frozen != null) {
            return CompletableFuture.failedFuture(frozen.cause());
        }
        while (true) {
            var entry = entries.computeIfAbsent(key, Entry::new);
            final CompletableFuture<ModelRenderTarget> loading;
            final Subscription subscription;
            synchronized (entry.lock) {
                if (entry.retired) {
                    entries.remove(key, entry);
                    continue;
                }
                entry.retention = Retention.stronger(entry.retention, retention);
                entry.lastUse = System.nanoTime();
                if (entry.renderTarget != null) {
                    entry.references++;
                    return CompletableFuture.completedFuture(lease(entry, entry.renderTarget));
                }
                if (entry.loading == null) {
                    entry.loadingScope = TaskScope.create(context.executor(), context.immediate());
                    final CompletableFuture<ModelRenderTarget> created;
                    try {
                        created = loader.load(entry.loadingScope);
                    } catch (Throwable error) {
                        entry.loadingScope.close();
                        entry.loadingScope = null;
                        entries.remove(key, entry);
                        if (!(unwrap(error) instanceof CancellationException)) {
                            failures.recordTarget(key, unwrap(error));
                        }
                        return CompletableFuture.failedFuture(error);
                    }
                    var result = new CompletableFuture<ModelRenderTarget>();
                    entry.loading = result;
                    loading = result;
                    created.whenComplete((renderTarget, error) -> {
                        finishLoad(key, entry, renderTarget, error);
                        if (error == null) {
                            result.complete(renderTarget);
                        } else {
                            result.completeExceptionally(error);
                        }
                    });
                } else {
                    loading = entry.loading;
                }
                subscription = new Subscription(entry);
                entry.subscriptions.add(subscription);
            }
            subscription.setGuard(context.guard(subscription));
            loading.whenComplete(subscription::complete);
            return subscription.result;
        }
    }

    public boolean isLoaded(ModelRenderTargetRequestKey key) {
        var entry = entries.get(key);
        if (entry == null) {
            return false;
        }
        synchronized (entry.lock) {
            return entry.renderTarget != null && !entry.retired;
        }
    }

    public void demoteSessionRetention() {
        entries.values().forEach(entry -> {
            synchronized (entry.lock) {
                if (entry.retention == Retention.SESSION) {
                    entry.retention = Retention.IDLE;
                }
            }
        });
    }

    public void invalidate(ModelContentVersion contentVersion) {
        entries.forEach((key, entry) -> {
            if (key.contentVersion().equals(contentVersion) && entries.remove(key, entry)) {
                retire(entry, true, false);
            }
        });
    }

    public void evictIdle() {
        var cutoff = System.nanoTime() - IDLE_NANOS;
        entries.forEach((key, entry) -> {
            final boolean remove;
            synchronized (entry.lock) {
                remove = entry.retention == Retention.IDLE && entry.references == 0
                        && entry.renderTarget != null && entry.lastUse <= cutoff;
            }
            if (remove && entries.remove(key, entry)) {
                retire(entry, false, false);
            }
        });
    }

    public void clearSession() {
        entries.forEach((key, entry) -> {
            final boolean permanent;
            synchronized (entry.lock) {
                permanent = entry.retention == Retention.PERMANENT;
            }
            if (!permanent && entries.remove(key, entry)) {
                retire(entry, true, false);
            }
        });
    }

    public int loadingCount() {
        var count = 0;
        for (var entry : entries.values()) {
            synchronized (entry.lock) {
                if (!entry.retired && entry.renderTarget == null && entry.loading != null) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public void close() {
        var copy = new ArrayList<>(entries.values());
        entries.clear();
        for (var entry : copy) {
            retire(entry, true, true);
        }
    }

    private void finishLoad(ModelRenderTargetRequestKey key, Entry entry,
                            ModelRenderTarget renderTarget, Throwable error) {
        TaskScope loadingScope;
        var close = false;
        synchronized (entry.lock) {
            loadingScope = entry.loadingScope;
            entry.loadingScope = null;
            entry.loading = null;
            if (error != null) {
                var cause = unwrap(error);
                if (cause instanceof CancellationException) {
                    entries.remove(key, entry);
                } else {
                    failures.recordTarget(key, cause);
                }
            } else if (entry.retired) {
                close = true;
            } else {
                entry.renderTarget = renderTarget;
                entry.lastUse = System.nanoTime();
            }
        }
        if (loadingScope != null) {
            loadingScope.close();
        }
        if (close) {
            closeOnRenderThread(renderTarget);
        }
    }

    private ModelRenderTargetLease lease(Entry entry, ModelRenderTarget renderTarget) {
        return new ModelRenderTargetLease(renderTarget, () -> release(entry), entry.validity::isCurrent);
    }

    private void release(Entry entry) {
        ModelRenderTarget close = null;
        synchronized (entry.lock) {
            if (entry.references <= 0) {
                throw new IllegalStateException("Unbalanced model renderTarget lease");
            }
            entry.references--;
            entry.lastUse = System.nanoTime();
            if (entry.retired && entry.references == 0 && entry.renderTarget != null) {
                close = entry.renderTarget;
                entry.renderTarget = null;
            }
        }
        if (close != null) {
            closeOnRenderThread(close);
        }
    }

    private static void retire(Entry entry, boolean cancelSubscribers, boolean forceTarget) {
        Subscription[] subscriptions = new Subscription[0];
        TaskScope loadingScope;
        ModelRenderTarget close = null;
        synchronized (entry.lock) {
            entry.validity.revoke();
            entry.retired = true;
            entry.retention = Retention.IDLE;
            loadingScope = entry.loadingScope;
            entry.loadingScope = null;
            if (cancelSubscribers) {
                subscriptions = entry.subscriptions.toArray(Subscription[]::new);
            }
            if (entry.renderTarget != null && (forceTarget || entry.references == 0)) {
                close = entry.renderTarget;
                entry.renderTarget = null;
            }
        }
        if (loadingScope != null) {
            loadingScope.close();
        }
        for (var subscription : subscriptions) {
            subscription.close();
        }
        if (close != null) {
            closeOnRenderThread(close);
        }
    }

    private static CompletableFuture<ModelRenderTargetLease> cancelledFuture() {
        return CompletableFuture.failedFuture(new CancellationException("Request scope was closed"));
    }

    private static Throwable unwrap(Throwable error) {
        while ((error instanceof CompletionException || error instanceof ExecutionException)
                && error.getCause() != null) {
            error = error.getCause();
        }
        return error;
    }

    private static void closeOnRenderThread(ModelRenderTarget renderTarget) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.isSameThread()) {
            renderTarget.close();
        } else {
            minecraft.execute(renderTarget::close);
        }
    }

    private final class Subscription implements AutoCloseable {
        private final Entry entry;
        private final CompletableFuture<ModelRenderTargetLease> result = new CompletableFuture<>();
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

        private void complete(ModelRenderTarget renderTarget, Throwable error) {
            if (!done.compareAndSet(false, true)) {
                return;
            }
            ModelRenderTargetLease nextLease = null;
            synchronized (entry.lock) {
                entry.subscriptions.remove(this);
                if (error == null && !entry.retired && entry.renderTarget == renderTarget) {
                    entry.references++;
                    entry.lastUse = System.nanoTime();
                    nextLease = lease(entry, renderTarget);
                }
            }
            closeGuard();
            if (nextLease != null) {
                result.complete(nextLease);
            } else if (error != null) {
                result.completeExceptionally(error);
            } else {
                result.completeExceptionally(
                        new CancellationException("Model render target request was retired"));
            }
        }

        @Override
        public void close() {
            if (!done.compareAndSet(false, true)) {
                return;
            }
            TaskScope loadingScope = null;
            synchronized (entry.lock) {
                entry.subscriptions.remove(this);
                if (entry.subscriptions.isEmpty() && entry.renderTarget == null && entry.loading != null
                        && !entry.retired) {
                    entries.remove(entry.key, entry);
                    entry.retired = true;
                    loadingScope = entry.loadingScope;
                    entry.loadingScope = null;
                }
            }
            if (loadingScope != null) {
                loadingScope.close();
            }
            closeGuard();
            result.completeExceptionally(new CancellationException("Request scope was closed"));
        }

        private void closeGuard() {
            var current = guard;
            guard = null;
            if (current != null) {
                current.close();
            }
        }
    }

    private static final class Entry {
        private final ModelRenderTargetRequestKey key;
        private final Object lock = new Object();
        private final LeaseValidity validity = new LeaseValidity();
        private final HashSet<Subscription> subscriptions = new HashSet<>();
        private CompletableFuture<ModelRenderTarget> loading;
        private TaskScope loadingScope;
        private ModelRenderTarget renderTarget;
        private int references;
        private long lastUse = System.nanoTime();
        private Retention retention = Retention.IDLE;
        private boolean retired;

        private Entry(ModelRenderTargetRequestKey key) {
            this.key = key;
        }
    }

    @FunctionalInterface
    public interface Loader {
        CompletableFuture<ModelRenderTarget> load(TaskContext context);
    }

    public enum Retention {
        IDLE,
        SESSION,
        PERMANENT;

        private static Retention stronger(Retention left, Retention right) {
            return left.ordinal() >= right.ordinal() ? left : right;
        }
    }
}
