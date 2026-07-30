package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.storage.ModelBackingIdentity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogReloadCoordinatorTest {
    private final ScheduledThreadPoolExecutor control = new ScheduledThreadPoolExecutor(1);
    private final java.util.concurrent.ExecutorService workers = Executors.newFixedThreadPool(2);

    @AfterEach
    void closeExecutors() {
        workers.shutdownNow();
        control.shutdownNow();
    }

    @Test
    void requestArrivingDuringActiveTransactionRunsInNextGeneration() throws Exception {
        var calls = new AtomicInteger();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var repository = new TestRepository();
        var coordinator = new CatalogReloadCoordinator(control, workers, (base, request) -> {
            if (calls.getAndIncrement() == 0) {
                entered.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new CatalogInfrastructureException("interrupted", error);
                }
            }
            return candidate(base);
        }, repository);

        var first = coordinator.reload();
        entered.await(5, TimeUnit.SECONDS);
        var second = coordinator.reload();
        release.countDown();

        assertEquals(1, first.get(5, TimeUnit.SECONDS).reloadGeneration());
        assertEquals(2, second.get(5, TimeUnit.SECONDS).reloadGeneration());
        assertEquals(2, calls.get());
        coordinator.closeAsync().get(5, TimeUnit.SECONDS);
    }

    @Test
    void closeCancelsActiveAndQueuedWaitersWithoutWaitingForReconcile() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var repository = new TestRepository();
        var coordinator = new CatalogReloadCoordinator(control, workers, (base, request) -> {
            entered.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new CatalogInfrastructureException("interrupted", error);
            }
            return candidate(base);
        }, repository);

        var active = coordinator.reload();
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        var queued = coordinator.reload();
        try {
            coordinator.closeAsync().get(1, TimeUnit.SECONDS);
            assertThrows(CancellationException.class, active::join);
            assertThrows(CancellationException.class, queued::join);
        } finally {
            release.countDown();
        }
    }

    @Test
    void durableRecoveryFutureStaysPendingAcrossInfrastructureBackoff() throws Exception {
        var calls = new AtomicInteger();
        var repository = new TestRepository();
        var coordinator = new CatalogReloadCoordinator(control, workers, (base, request) -> {
            if (calls.getAndIncrement() == 0) {
                throw new CatalogInfrastructureException("cache unavailable");
            }
            return candidate(base);
        }, repository);
        var backing = new CatalogBackingKey(
                new CatalogModelLocation(CatalogRootKind.CUSTOM,
                        new com.elfmcys.ysm.model.domain.ModelPath("model")),
                new ModelBackingIdentity.ConvertedObject(Path.of("object.mxc"), hash(1)));

        var recovery = coordinator.recoverUntilSettled(backing);
        Thread.sleep(200);
        assertFalse(recovery.isDone());
        assertEquals(1, recovery.get(4, TimeUnit.SECONDS).reloadGeneration());
        assertEquals(2, calls.get());
        coordinator.closeAsync().get(5, TimeUnit.SECONDS);
    }

    @Test
    void cancelledDurableRecoveryDoesNotStartAnotherBackoffAttempt() throws Exception {
        var calls = new AtomicInteger();
        var failed = new CountDownLatch(1);
        var repository = new TestRepository();
        var coordinator = new CatalogReloadCoordinator(control, workers, (base, request) -> {
            calls.incrementAndGet();
            failed.countDown();
            throw new CatalogInfrastructureException("cache unavailable");
        }, repository);
        var backing = new CatalogBackingKey(
                new CatalogModelLocation(CatalogRootKind.CUSTOM,
                        new com.elfmcys.ysm.model.domain.ModelPath("model")),
                new ModelBackingIdentity.ConvertedObject(Path.of("object.mxc"), hash(2)));

        var recovery = coordinator.recoverUntilSettled(backing);
        failed.await(5, TimeUnit.SECONDS);
        recovery.cancel(false);
        Thread.sleep(1_300);

        assertEquals(1, calls.get());
        coordinator.closeAsync().get(5, TimeUnit.SECONDS);
    }

    @Test
    void duplicateRecoverySharesOneInvalidationAndReconcile() throws Exception {
        var calls = new AtomicInteger();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var repository = new TestRepository();
        var coordinator = new CatalogReloadCoordinator(control, workers, (base, request) -> {
            calls.incrementAndGet();
            entered.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new CatalogInfrastructureException("interrupted", error);
            }
            return candidate(base);
        }, repository);
        var backing = backing(3);

        var first = coordinator.recoverUntilSettled(backing);
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        var duplicate = coordinator.recoverUntilSettled(backing);
        control.submit(() -> { }).get(5, TimeUnit.SECONDS);
        release.countDown();

        assertEquals(1, first.get(5, TimeUnit.SECONDS).reloadGeneration());
        assertEquals(1, duplicate.get(5, TimeUnit.SECONDS).reloadGeneration());
        assertEquals(1, repository.preparedRecoveries.get());
        assertEquals(1, calls.get());
        coordinator.closeAsync().get(5, TimeUnit.SECONDS);
    }

    @Test
    void durableAuditKeepsSystemRepairReasonAcrossRetry() throws Exception {
        var calls = new AtomicInteger();
        var reasons = java.util.Collections.synchronizedList(
                new java.util.ArrayList<Set<ReloadReason>>());
        var repository = new TestRepository();
        var coordinator = new CatalogReloadCoordinator(control, workers, (base, request) -> {
            reasons.add(request.reasons());
            if (calls.getAndIncrement() == 0) {
                throw new CatalogInfrastructureException("cache unavailable");
            }
            return candidate(base);
        }, repository);

        assertEquals(1, coordinator.auditUntilSettled()
                .get(4, TimeUnit.SECONDS).reloadGeneration());
        assertEquals(List.of(Set.of(ReloadReason.SYSTEM_REPAIR),
                Set.of(ReloadReason.SYSTEM_REPAIR)), reasons);
        coordinator.closeAsync().get(5, TimeUnit.SECONDS);
    }

    private static CatalogReconcileResult candidate(ReloadableCatalogSnapshot base) {
        var snapshot = new ReloadableCatalogSnapshot(base.reloadGeneration() + 1, true,
                java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), List.of(),
                com.elfmcys.ysm.model.domain.ModelScanReport.empty());
        return new CatalogReconcileResult(snapshot, Set.of(), Set.of(), ReloadStats.empty());
    }

    private static ModelHash hash(int seed) {
        var bytes = new byte[ModelHash.SIZE];
        bytes[0] = (byte) seed;
        return new ModelHash(bytes);
    }

    private static CatalogBackingKey backing(int seed) {
        return new CatalogBackingKey(
                new CatalogModelLocation(CatalogRootKind.CUSTOM,
                        new com.elfmcys.ysm.model.domain.ModelPath("model")),
                new ModelBackingIdentity.ConvertedObject(Path.of("object.mxc"), hash(seed)));
    }

    private static final class TestRepository implements CatalogReloadCoordinator.Repository {
        private volatile ReloadableCatalogSnapshot current = ReloadableCatalogSnapshot.unready();
        private final AtomicInteger preparedRecoveries = new AtomicInteger();

        @Override
        public ReloadableCatalogSnapshot current() {
            return current;
        }

        @Override
        public boolean prepareRecovery(CatalogBackingKey backing) {
            preparedRecoveries.incrementAndGet();
            return true;
        }

        @Override
        public ReloadResult commit(ReloadableCatalogSnapshot base,
                                   CatalogReconcileResult candidate) {
            current = candidate.snapshot();
            return new ReloadResult(current.reloadGeneration(), true, true,
                    0, 0, 0, candidate.stats(), "");
        }
    }
}
