package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.importer.LegacyImporter;
import com.elfmcys.ysm.model.importer.RawModelImporter;
import com.elfmcys.ysm.model.storage.ModelBackingIdentity;
import com.elfmcys.ysm.model.storage.ModelStorageInfrastructure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** The single physical-JVM owner of reloadable custom/auth catalog state. */
public final class ReloadableModelCatalog implements AutoCloseable,
        CatalogReloadCoordinator.Repository {
    private final ScheduledThreadPoolExecutor control;
    private final ScheduledThreadPoolExecutor workers;
    private final CatalogReloadCoordinator coordinator;
    private final ModelDirectoryWatcher watcher;
    private final Consumer<Hash256> convertedInvalidator;
    private final Map<Long, SubscriberState> subscribers = new LinkedHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile ReloadableCatalogSnapshot current = ReloadableCatalogSnapshot.unready();
    private long nextSubscriberId = 1;

    public ReloadableModelCatalog(ModelStorageInfrastructure storage,
                                  List<ModelCatalogSource> roots,
                                  Set<Hash256> builtinReservedHashes,
                                  BuiltinModelIndex builtinContract) {
        control = new ScheduledThreadPoolExecutor(1, runnable -> {
            var thread = new Thread(runnable, "YSM Model Catalog Control");
            thread.setDaemon(true);
            return thread;
        });
        control.setRemoveOnCancelPolicy(true);
        var workerCount = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        workers = new ScheduledThreadPoolExecutor(workerCount, runnable -> {
            var thread = new Thread(runnable, "YSM Model Catalog Worker");
            thread.setDaemon(true);
            return thread;
        });
        workers.setRemoveOnCancelPolicy(true);

        var importer = new RawModelImporter(new LegacyImporter(), builtinContract);
        var objects = storage.objects();
        convertedInvalidator = objects::invalidate;
        var resolver = new ModelSourceResolver(importer, storage.indexes(), objects);
        var reconciler = new CatalogReconciler(roots, resolver, builtinReservedHashes);
        coordinator = new CatalogReloadCoordinator(control, workers, reconciler, this);
        try {
            watcher = new ModelDirectoryWatcher(roots, coordinator::watcherChanged);
        } catch (IOException error) {
            workers.shutdownNow();
            control.shutdownNow();
            throw new UncheckedIOException("Failed to start the reloadable model watcher", error);
        }
        coordinator.start();
    }

    ReloadableModelCatalog(ScheduledThreadPoolExecutor control,
                           ScheduledThreadPoolExecutor workers,
                           CatalogReloadCoordinator.ReconcileAction reconcile,
                           Consumer<Hash256> convertedInvalidator) {
        this.control = Objects.requireNonNull(control, "control");
        this.workers = Objects.requireNonNull(workers, "workers");
        this.convertedInvalidator = Objects.requireNonNull(
                convertedInvalidator, "convertedInvalidator");
        coordinator = new CatalogReloadCoordinator(control, workers, reconcile, this);
        watcher = null;
    }

    @Override
    public ReloadableCatalogSnapshot current() {
        return current;
    }

    public CompletableFuture<ReloadResult> reload() {
        requireOpen();
        return coordinator.reload();
    }

    public CompletableFuture<ReloadResult> recoverUntilSettled(CatalogBackingKey backing) {
        Objects.requireNonNull(backing, "backing");
        requireOpen();
        return coordinator.recoverUntilSettled(backing);
    }

    public CompletableFuture<ReloadResult> auditUntilSettled() {
        requireOpen();
        return coordinator.auditUntilSettled();
    }

    public CompletableFuture<Subscription> subscribe(Executor executor,
                                                     TransitionSubscriber subscriber) {
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(subscriber, "subscriber");
        requireOpen();
        var completion = new CompletableFuture<Subscription>();
        control.execute(() -> {
            if (closed.get()) {
                completion.completeExceptionally(
                        new IllegalStateException("Reloadable model catalog is closed"));
                return;
            }
            var id = nextSubscriberId++;
            var state = new SubscriberState(id, executor, subscriber);
            subscribers.put(id, state);
            dispatch(state, ReloadableCatalogTransition.initial(current));
            completion.complete(new Subscription(state));
        });
        return completion;
    }

    @Override
    public ReloadResult commit(ReloadableCatalogSnapshot base,
                               CatalogReconcileResult candidate) {
        if (current.reloadGeneration() != base.reloadGeneration()) {
            throw new IllegalStateException("Reloadable catalog base generation changed");
        }
        var next = candidate.snapshot();
        var transition = new ReloadableCatalogTransition(base, next,
                candidate.touchedDirectBackings(), candidate.revalidatedBackings(),
                candidate.stats());
        var changed = !samePublishedCatalog(base, next)
                || !transition.touchedDirectBackings().isEmpty()
                || !transition.revalidatedBackings().isEmpty();
        current = next;
        if (changed) {
            subscribers.values().forEach(subscriber -> dispatch(subscriber, transition));
        } else {
            subscribers.values().forEach(subscriber -> advanceWithoutDispatch(
                    subscriber, next.reloadGeneration()));
        }
        return new ReloadResult(next.reloadGeneration(), true, changed,
                next.models().size(), next.packs().size(), next.report().errorCount(),
                candidate.stats(), "");
    }

    private void dispatch(SubscriberState state, ReloadableCatalogTransition transition) {
        if (state.closed) {
            return;
        }
        state.mailbox = state.mailbox.handle((ignored, error) -> null)
                .thenComposeAsync(ignored -> {
                    if (state.closed) {
                        return CompletableFuture.completedFuture(null);
                    }
                    try {
                        return Objects.requireNonNull(state.subscriber.apply(transition),
                                "subscriber result");
                    } catch (Throwable error) {
                        return CompletableFuture.failedFuture(error);
                    }
                }, state.executor);
        var applied = state.mailbox;
        applied.whenComplete((ignored, error) -> control.execute(() -> {
            if (error == null) {
                acknowledge(state, transition.current().reloadGeneration());
            } else {
                reject(state, transition.current().reloadGeneration(), unwrap(error));
            }
        }));
    }

    private void advanceWithoutDispatch(SubscriberState state, long generation) {
        if (state.closed) {
            return;
        }
        state.mailbox = state.mailbox.thenRun(() -> { });
        var applied = state.mailbox;
        applied.whenComplete((ignored, error) -> control.execute(() -> {
            if (error == null) {
                acknowledge(state, generation);
            } else {
                reject(state, generation, unwrap(error));
            }
        }));
    }

    private static boolean samePublishedCatalog(ReloadableCatalogSnapshot left,
                                                ReloadableCatalogSnapshot right) {
        if (left.ready() != right.ready() || !left.packs().equals(right.packs())
                || left.models().size() != right.models().size()) {
            return false;
        }
        for (var entry : left.models().entrySet()) {
            var other = right.models().get(entry.getKey());
            if (other == null
                    || !entry.getValue().location().equals(other.location())
                    || !entry.getValue().backingIdentity().equals(other.backingIdentity())
                    || !entry.getValue().descriptor().descriptorHash()
                    .equals(other.descriptor().descriptorHash())) {
                return false;
            }
        }
        return true;
    }

    private void acknowledge(SubscriberState state, long generation) {
        if (state.closed) {
            return;
        }
        state.appliedGeneration = Math.max(state.appliedGeneration, generation);
        state.failures.keySet().removeIf(value -> value <= state.appliedGeneration);
        var completed = new ArrayList<Long>();
        state.waiters.forEach((target, futures) -> {
            if (target <= state.appliedGeneration) {
                futures.forEach(future -> future.complete(null));
                completed.add(target);
            }
        });
        completed.forEach(state.waiters::remove);
    }

    private void reject(SubscriberState state, long generation, Throwable error) {
        if (state.closed || generation <= state.appliedGeneration) {
            return;
        }
        state.failures.put(generation, error);
        var waiters = state.waiters.remove(generation);
        if (waiters != null) {
            waiters.forEach(future -> future.completeExceptionally(error));
        }
    }

    private CompletableFuture<Void> awaitApplied(SubscriberState state, long generation) {
        var completion = new CompletableFuture<Void>();
        control.execute(() -> {
            if (state.closed) {
                completion.completeExceptionally(
                        new IllegalStateException("Catalog subscription is closed"));
            } else if (generation <= state.appliedGeneration) {
                completion.complete(null);
            } else if (state.failures.containsKey(generation)) {
                completion.completeExceptionally(state.failures.get(generation));
            } else {
                state.waiters.computeIfAbsent(generation, ignored -> new ArrayList<>())
                        .add(completion);
            }
        });
        return completion;
    }

    private void closeSubscription(SubscriberState state) {
        control.execute(() -> {
            if (state.closed) {
                return;
            }
            state.closed = true;
            subscribers.remove(state.id);
            var error = new IllegalStateException("Catalog subscription is closed");
            state.waiters.values().forEach(values ->
                    values.forEach(future -> future.completeExceptionally(error)));
            state.waiters.clear();
            state.failures.clear();
        });
    }

    private static Throwable unwrap(Throwable error) {
        var current = error;
        while (current instanceof java.util.concurrent.CompletionException
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void requireOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Reloadable model catalog is closed");
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (watcher != null) {
            watcher.close();
        }
        coordinator.closeAsync().join();
        var drained = new CompletableFuture<Void>();
        control.execute(() -> {
            subscribers.values().forEach(state -> {
                state.closed = true;
                var error = new IllegalStateException("Reloadable model catalog is closed");
                state.waiters.values().forEach(values ->
                        values.forEach(future -> future.completeExceptionally(error)));
            });
            subscribers.clear();
            drained.complete(null);
        });
        drained.join();
        workers.shutdown();
        control.shutdown();
        try {
            stopExecutor(workers, "model catalog workers");
            stopExecutor(control, "model catalog control");
        } catch (InterruptedException error) {
            workers.shutdownNow();
            control.shutdownNow();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while closing the model catalog", error);
        }
    }

    private static void stopExecutor(ScheduledThreadPoolExecutor executor,
                                     String name) throws InterruptedException {
        if (executor.awaitTermination(30, TimeUnit.SECONDS)) {
            return;
        }
        executor.shutdownNow();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Failed to stop " + name);
        }
    }

    @Override
    public boolean prepareRecovery(CatalogBackingKey backing) {
        if (current.find(backing).isEmpty()) {
            return false;
        }
        if (backing.identity() instanceof ModelBackingIdentity.ConvertedObject converted) {
            convertedInvalidator.accept(converted.modelHash());
        }
        return true;
    }

    @FunctionalInterface
    public interface TransitionSubscriber {
        CompletableFuture<Void> apply(ReloadableCatalogTransition transition);
    }

    public final class Subscription implements AutoCloseable {
        private final SubscriberState state;

        private Subscription(SubscriberState state) {
            this.state = state;
        }

        public CompletableFuture<Void> awaitApplied(long generation) {
            return ReloadableModelCatalog.this.awaitApplied(state, generation);
        }

        @Override
        public void close() {
            closeSubscription(state);
        }
    }

    private static final class SubscriberState {
        private final long id;
        private final Executor executor;
        private final TransitionSubscriber subscriber;
        private CompletableFuture<Void> mailbox = CompletableFuture.completedFuture(null);
        private final TreeMap<Long, List<CompletableFuture<Void>>> waiters = new TreeMap<>();
        private final Map<Long, Throwable> failures = new HashMap<>();
        private long appliedGeneration = -1;
        private boolean closed;

        private SubscriberState(long id, Executor executor,
                                TransitionSubscriber subscriber) {
            this.id = id;
            this.executor = executor;
            this.subscriber = subscriber;
        }
    }
}
