package com.elfmcys.ysm.model.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadableModelCatalogTest {
    @Test
    void trueNoOpAdvancesAcknowledgementWithoutDispatchingSubscriber() throws Exception {
        var control = new ScheduledThreadPoolExecutor(1);
        var workers = new ScheduledThreadPoolExecutor(1);
        var catalog = new ReloadableModelCatalog(control, workers,
                (base, request) -> candidate(base), ignored -> { });
        var subscriberCalls = new AtomicInteger();
        try {
            var subscription = catalog.subscribe(Runnable::run, transition -> {
                subscriberCalls.incrementAndGet();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }).get(5, TimeUnit.SECONDS);
            assertEquals(1, subscriberCalls.get());

            var initial = catalog.reload().get(5, TimeUnit.SECONDS);
            assertTrue(initial.catalogChanged());
            subscription.awaitApplied(initial.reloadGeneration()).get(5, TimeUnit.SECONDS);
            assertEquals(2, subscriberCalls.get());

            var noOp = catalog.reload().get(5, TimeUnit.SECONDS);
            assertFalse(noOp.catalogChanged());
            subscription.awaitApplied(noOp.reloadGeneration()).get(5, TimeUnit.SECONDS);
            assertEquals(2, subscriberCalls.get());
            assertEquals(2, catalog.current().reloadGeneration());
        } finally {
            catalog.close();
        }
    }

    private static CatalogReconcileResult candidate(ReloadableCatalogSnapshot base) {
        var snapshot = new ReloadableCatalogSnapshot(base.reloadGeneration() + 1, true,
                java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), List.of(),
                com.elfmcys.ysm.model.domain.ModelScanReport.empty());
        return new CatalogReconcileResult(snapshot, Set.of(), Set.of(), ReloadStats.empty());
    }
}
