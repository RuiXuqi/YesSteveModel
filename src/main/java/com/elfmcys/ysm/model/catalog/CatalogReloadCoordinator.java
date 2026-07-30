package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.YesSteveModel;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Serializes every reload trigger while source resolution runs on model workers. */
public final class CatalogReloadCoordinator implements AutoCloseable {
    static final Duration QUIET_PERIOD = Duration.ofMillis(400);
    static final Duration MAX_WATCH_DELAY = Duration.ofSeconds(2);
    static final int PATH_LIMIT = 1024;
    private static final long[] RETRY_SECONDS = {1, 2, 5, 10, 30};

    private final ScheduledExecutorService control;
    private final Executor workers;
    private final ReconcileAction reconcile;
    private final Repository repository;
    private final Map<CatalogBackingKey, RecoveryOperation> recoveries = new HashMap<>();

    private Batch debounced;
    private Batch pending;
    private Batch active;
    private ScheduledFuture<?> scheduled;
    private long firstWatchNanos;
    private int retryIndex;
    private boolean running;
    private boolean closed;
    private final CompletableFuture<Void> closedFuture = new CompletableFuture<>();

    public CatalogReloadCoordinator(ScheduledExecutorService control, Executor workers,
                                    CatalogReconciler reconciler, Repository repository) {
        this.control = control;
        this.workers = workers;
        this.reconcile = reconciler::reconcile;
        this.repository = repository;
    }

    CatalogReloadCoordinator(ScheduledExecutorService control, Executor workers,
                             ReconcileAction reconcile, Repository repository) {
        this.control = control;
        this.workers = workers;
        this.reconcile = reconcile;
        this.repository = repository;
    }

    public void start() {
        enqueue(new Batch(ReloadRequest.startup(), List.of()), false);
    }

    public void watcherChanged(SourceChangeSet changes) {
        var reason = changes.overflow() ? ReloadReason.WATCH_OVERFLOW : ReloadReason.WATCH_EVENT;
        var level = changes.overflow() ? AuditLevel.FULL_CONTENT : AuditLevel.INCREMENTAL;
        var paths = changes.overflow() ? Set.<Path>of() : changes.paths();
        enqueue(new Batch(new ReloadRequest(Set.of(reason), level, paths, Set.of()), List.of()),
                true);
    }

    public CompletableFuture<ReloadResult> reload() {
        var completion = new CompletableFuture<ReloadResult>();
        enqueue(new Batch(ReloadRequest.manual(),
                List.of(new Waiter(completion, false))), false);
        return completion;
    }

    public CompletableFuture<ReloadResult> recoverUntilSettled(CatalogBackingKey backing) {
        Objects.requireNonNull(backing, "backing");
        var completion = new CompletableFuture<ReloadResult>();
        control.execute(() -> acceptRecovery(backing, completion));
        return completion;
    }

    public CompletableFuture<ReloadResult> auditUntilSettled() {
        var completion = new CompletableFuture<ReloadResult>();
        var request = new ReloadRequest(Set.of(ReloadReason.SYSTEM_REPAIR),
                AuditLevel.FULL_CONTENT, Set.of(), Set.of());
        enqueue(new Batch(request, List.of(new Waiter(completion, true))), false);
        return completion;
    }

    private void acceptRecovery(CatalogBackingKey backing,
                                CompletableFuture<ReloadResult> completion) {
        if (closed) {
            cancel(completion);
            return;
        }
        if (completion.isDone()) {
            return;
        }
        var activeRecovery = recoveries.get(backing);
        if (activeRecovery != null) {
            attach(activeRecovery, completion);
            return;
        }

        final boolean prepared;
        try {
            prepared = repository.prepareRecovery(backing);
        } catch (RuntimeException error) {
            completion.completeExceptionally(error);
            return;
        }
        if (!prepared) {
            completion.complete(ReloadResult.unchanged(repository.current()));
            return;
        }

        var recovery = new RecoveryOperation(new CompletableFuture<>());
        recoveries.put(backing, recovery);
        attach(recovery, completion);
        recovery.coordinatorCompletion().whenComplete((result, error) ->
                settleRecovery(backing, recovery, result, error));
        var request = new ReloadRequest(Set.of(ReloadReason.BACKING_RECOVERY),
                AuditLevel.FULL_CONTENT, Set.of(),
                Set.of(new BackingRecoveryRequest(backing)));
        accept(new Batch(request,
                List.of(new Waiter(recovery.coordinatorCompletion(), true))), false);
    }

    private void attach(RecoveryOperation recovery,
                        CompletableFuture<ReloadResult> completion) {
        recovery.completions().add(completion);
        completion.whenComplete((ignored, error) -> {
            if (completion.isCancelled()) {
                control.execute(() -> detachCancelled(recovery, completion));
            }
        });
    }

    private void detachCancelled(RecoveryOperation recovery,
                                 CompletableFuture<ReloadResult> completion) {
        recovery.completions().remove(completion);
        if (!recovery.completions().isEmpty()
                || recovery.coordinatorCompletion().isDone()) {
            return;
        }
        recoveries.values().removeIf(value -> value == recovery);
        recovery.coordinatorCompletion().cancel(false);
    }

    private void settleRecovery(CatalogBackingKey backing, RecoveryOperation recovery,
                                ReloadResult result, Throwable error) {
        recoveries.remove(backing, recovery);
        for (var completion : recovery.completions()) {
            if (error == null) {
                completion.complete(result);
            } else {
                completion.completeExceptionally(error);
            }
        }
        recovery.completions().clear();
    }

    private void enqueue(Batch batch, boolean debounceWatch) {
        control.execute(() -> accept(batch, debounceWatch));
    }

    private void accept(Batch batch, boolean debounceWatch) {
        if (closed) {
            cancel(batch);
            running = false;
            active = null;
            closedFuture.complete(null);
            return;
        }
        if (running) {
            pending = merge(pending, batch);
            return;
        }
        if (debounceWatch && pending != null) {
            pending = merge(pending, batch);
            return;
        }
        if (debounceWatch && !hasUrgentWaiter(batch)) {
            if (firstWatchNanos == 0) {
                firstWatchNanos = System.nanoTime();
            }
            debounced = merge(debounced, batch);
            scheduleDebounced();
            return;
        }

        batch = merge(debounced, batch);
        debounced = null;
        firstWatchNanos = 0;
        cancelScheduled();
        begin(batch);
    }

    private void scheduleDebounced() {
        cancelScheduled();
        var elapsed = System.nanoTime() - firstWatchNanos;
        var delay = Math.min(QUIET_PERIOD.toNanos(),
                Math.max(0, MAX_WATCH_DELAY.toNanos() - elapsed));
        scheduled = control.schedule(() -> {
            var next = debounced;
            debounced = null;
            firstWatchNanos = 0;
            scheduled = null;
            if (next != null) {
                begin(next);
            }
        }, delay, TimeUnit.NANOSECONDS);
    }

    private void begin(Batch batch) {
        batch = pruneCompletedWaiters(batch);
        if (batch == null || closed) {
            return;
        }
        var frozen = batch;
        running = true;
        active = frozen;
        var base = repository.current();
        CompletableFuture.supplyAsync(() -> {
            try {
                return reconcile.reconcile(base, frozen.request());
            } catch (CatalogBuildException error) {
                throw new CatalogReloadFailure(error);
            }
        }, workers).whenComplete((candidate, error) ->
                control.execute(() -> finish(base, frozen, candidate, error)));
    }

    private void finish(ReloadableCatalogSnapshot base, Batch batch,
                        CatalogReconcileResult candidate, Throwable error) {
        if (closed) {
            cancel(batch);
            running = false;
            active = null;
            closedFuture.complete(null);
            return;
        }
        running = false;
        active = null;
        if (error == null) {
            retryIndex = 0;
            final ReloadResult result;
            try {
                result = repository.commit(base, candidate);
            } catch (RuntimeException commitError) {
                fail(batch, commitError);
                return;
            }
            batch.waiters().forEach(waiter -> waiter.completion().complete(result));
            startPendingImmediately();
            return;
        }
        fail(batch, unwrap(error));
    }

    private void fail(Batch batch, Throwable error) {
        var current = repository.current();
        var failed = ReloadResult.failed(current,
                error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
        var retry = retryBatch(batch);
        batch.waiters().stream().filter(waiter -> !waiter.durable())
                .forEach(waiter -> waiter.completion().complete(failed));
        var arrivedWhileRunning = pending;
        pending = merge(retry, arrivedWhileRunning);
        if (error instanceof CatalogInventoryChangedException) {
            pending = merge(pending, automaticFullAudit());
        }
        YesSteveModel.LOGGER.error(
                "Model catalog reconcile failed; the previous snapshot remains active", error);

        if (arrivedWhileRunning != null && hasUrgentWaiter(arrivedWhileRunning)) {
            startPendingImmediately();
            return;
        }
        var delay = RETRY_SECONDS[Math.min(retryIndex++, RETRY_SECONDS.length - 1)];
        cancelScheduled();
        scheduled = control.schedule(() -> {
            scheduled = null;
            startPendingImmediately();
        }, delay, TimeUnit.SECONDS);
        YesSteveModel.LOGGER.error(
                "Model catalog infrastructure retry scheduled in {}s", delay);
    }

    private void startPendingImmediately() {
        if (closed || running) {
            return;
        }
        var next = merge(debounced, pending);
        debounced = null;
        pending = null;
        firstWatchNanos = 0;
        cancelScheduled();
        if (next != null) {
            begin(next);
        }
    }

    private static Batch retryBatch(Batch batch) {
        var durable = batch.waiters().stream()
                .filter(Waiter::durable)
                .filter(waiter -> !waiter.completion().isDone()).toList();
        var automatic = batch.request().reasons().stream().anyMatch(reason ->
                reason == ReloadReason.STARTUP || reason == ReloadReason.WATCH_EVENT
                        || reason == ReloadReason.WATCH_OVERFLOW);
        if (durable.isEmpty() && !automatic) {
            return null;
        }
        var reasons = new HashSet<ReloadReason>();
        if (automatic) {
            reasons.addAll(batch.request().reasons());
        }
        if (!durable.isEmpty()) {
            reasons.addAll(batch.request().reasons());
        }
        return new Batch(new ReloadRequest(reasons, AuditLevel.FULL_CONTENT,
                Set.of(), durable.isEmpty() ? Set.of() : batch.request().recoveries()), durable);
    }

    private static Batch automaticFullAudit() {
        return new Batch(new ReloadRequest(Set.of(ReloadReason.WATCH_OVERFLOW),
                AuditLevel.FULL_CONTENT, Set.of(), Set.of()), List.of());
    }

    private static Batch merge(Batch left, Batch right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        var reasons = new HashSet<>(left.request().reasons());
        reasons.addAll(right.request().reasons());
        var recoveries = new HashSet<>(left.request().recoveries());
        recoveries.addAll(right.request().recoveries());
        var full = left.request().auditLevel() == AuditLevel.FULL_CONTENT
                || right.request().auditLevel() == AuditLevel.FULL_CONTENT
                || reasons.contains(ReloadReason.WATCH_OVERFLOW);
        var paths = new HashSet<Path>();
        if (!full) {
            paths.addAll(left.request().touchedPaths());
            paths.addAll(right.request().touchedPaths());
            if (paths.size() > PATH_LIMIT) {
                full = true;
                paths.clear();
                reasons.add(ReloadReason.WATCH_OVERFLOW);
            }
        }
        var waiters = new ArrayList<>(left.waiters());
        waiters.addAll(right.waiters());
        return new Batch(new ReloadRequest(reasons,
                full ? AuditLevel.FULL_CONTENT : AuditLevel.INCREMENTAL,
                paths, recoveries), waiters);
    }

    private static boolean hasUrgentWaiter(Batch batch) {
        return batch.waiters().stream().anyMatch(waiter -> !waiter.completion().isDone());
    }

    private static Batch pruneCompletedWaiters(Batch batch) {
        if (batch == null) {
            return null;
        }
        var waiters = batch.waiters().stream()
                .filter(waiter -> !waiter.completion().isDone()).toList();
        if (!waiters.isEmpty()) {
            return waiters.size() == batch.waiters().size()
                    ? batch : new Batch(batch.request(), waiters);
        }
        var reasons = batch.request().reasons().stream().filter(reason ->
                reason == ReloadReason.STARTUP || reason == ReloadReason.WATCH_EVENT
                        || reason == ReloadReason.WATCH_OVERFLOW).collect(
                java.util.stream.Collectors.toUnmodifiableSet());
        if (reasons.isEmpty()) {
            return null;
        }
        return new Batch(new ReloadRequest(reasons, batch.request().auditLevel(),
                batch.request().touchedPaths(), Set.of()), List.of());
    }

    private static Throwable unwrap(Throwable error) {
        var current = error;
        while ((current instanceof java.util.concurrent.CompletionException
                || current instanceof CatalogReloadFailure) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void cancelScheduled() {
        if (scheduled != null) {
            scheduled.cancel(false);
            scheduled = null;
        }
    }

    private static void cancel(Batch batch) {
        if (batch == null) {
            return;
        }
        var cancellation = new CancellationException("Model catalog coordinator is closed");
        batch.waiters().forEach(waiter -> waiter.completion().completeExceptionally(cancellation));
    }

    private static void cancel(CompletableFuture<?> completion) {
        completion.completeExceptionally(
                new CancellationException("Model catalog coordinator is closed"));
    }

    @Override
    public void close() {
        closeAsync();
    }

    public CompletableFuture<Void> closeAsync() {
        control.execute(() -> {
            if (closed) {
                return;
            }
            closed = true;
            cancelScheduled();
            cancel(debounced);
            cancel(pending);
            cancel(active);
            debounced = null;
            pending = null;
            active = null;
            running = false;
            List.copyOf(recoveries.values()).forEach(recovery ->
                    recovery.coordinatorCompletion().completeExceptionally(
                            new CancellationException("Model catalog coordinator is closed")));
            recoveries.clear();
            closedFuture.complete(null);
        });
        return closedFuture;
    }

    public interface Repository {
        ReloadableCatalogSnapshot current();

        boolean prepareRecovery(CatalogBackingKey backing);

        ReloadResult commit(ReloadableCatalogSnapshot base, CatalogReconcileResult candidate);
    }

    @FunctionalInterface
    interface ReconcileAction {
        CatalogReconcileResult reconcile(ReloadableCatalogSnapshot base,
                                         ReloadRequest request) throws CatalogBuildException;
    }

    private record Batch(ReloadRequest request, List<Waiter> waiters) {
        private Batch {
            waiters = List.copyOf(waiters);
        }
    }

    private record Waiter(CompletableFuture<ReloadResult> completion, boolean durable) {
    }

    private record RecoveryOperation(CompletableFuture<ReloadResult> coordinatorCompletion,
                                     Set<CompletableFuture<ReloadResult>> completions) {
        private RecoveryOperation(CompletableFuture<ReloadResult> coordinatorCompletion) {
            this(coordinatorCompletion, new LinkedHashSet<>());
        }
    }

    private static final class CatalogReloadFailure extends RuntimeException {
        private CatalogReloadFailure(CatalogBuildException cause) {
            super(cause);
        }
    }
}
