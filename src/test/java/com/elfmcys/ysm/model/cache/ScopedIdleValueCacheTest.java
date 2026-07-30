package com.elfmcys.ysm.model.cache;

import com.elfmcys.ysm.task.TaskContext;
import com.elfmcys.ysm.task.TaskScope;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScopedIdleValueCacheTest {
    @Test
    void cancelsLoaderOnlyAfterLastPendingSubscriberLeaves() {
        var loads = new AtomicInteger();
        var loaderContext = new AtomicReference<TaskContext>();
        var pending = new CompletableFuture<String>();
        try (var cache = new ScopedIdleValueCache<String, String>(
                Duration.ofSeconds(30), ignored -> { });
             var firstScope = TaskScope.create(Runnable::run);
             var secondScope = TaskScope.create(Runnable::run)) {
            var first = cache.get(firstScope, "model", (context, key) -> {
                loads.incrementAndGet();
                loaderContext.set(context);
                return pending;
            });
            var second = cache.get(secondScope, "model", (context, key) -> {
                loads.incrementAndGet();
                return pending;
            });

            assertEquals(1, loads.get());
            firstScope.close();
            assertThrows(CancellationException.class, first::join);
            assertFalse(loaderContext.get().cancelled());

            secondScope.close();
            assertThrows(CancellationException.class, second::join);
            assertTrue(loaderContext.get().cancelled());
            assertEquals(0, cache.size());
        }
    }

    @Test
    void completedValueRemainsReusableAfterSubscriberScopeCloses() {
        var loads = new AtomicInteger();
        try (var cache = new ScopedIdleValueCache<String, String>(
                Duration.ofSeconds(30), ignored -> { });
             var firstScope = TaskScope.create(Runnable::run);
             var secondScope = TaskScope.create(Runnable::run)) {
            assertEquals("ready", cache.get(firstScope, "model", (context, key) -> {
                loads.incrementAndGet();
                return CompletableFuture.completedFuture("ready");
            }).join());
            firstScope.close();

            assertEquals("ready", cache.get(secondScope, "model", (context, key) -> {
                loads.incrementAndGet();
                return CompletableFuture.completedFuture("other");
            }).join());
            assertEquals(1, loads.get());
        }
    }

    @Test
    void immediatelyFailedLoadIsRemovedAndCanBeRetried() {
        var loads = new AtomicInteger();
        try (var cache = new ScopedIdleValueCache<String, String>(
                Duration.ofSeconds(30), ignored -> { });
             var firstScope = TaskScope.create(Runnable::run);
             var secondScope = TaskScope.create(Runnable::run)) {
            var failed = cache.get(firstScope, "model", (context, key) -> {
                loads.incrementAndGet();
                return CompletableFuture.failedFuture(new IllegalStateException("failed"));
            });
            assertThrows(CompletionException.class, failed::join);

            assertEquals("ready", cache.get(secondScope, "model", (context, key) -> {
                loads.incrementAndGet();
                return CompletableFuture.completedFuture("ready");
            }).join());
            assertEquals(2, loads.get());
        }
    }
}
