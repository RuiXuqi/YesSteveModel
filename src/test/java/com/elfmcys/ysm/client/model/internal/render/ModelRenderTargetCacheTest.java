package com.elfmcys.ysm.client.model.internal.render;

import com.elfmcys.ysm.client.model.ModelRenderTarget;
import com.elfmcys.ysm.client.model.catalog.ModelContentVersion;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.task.TaskContext;
import com.elfmcys.ysm.task.TaskScope;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelRenderTargetCacheTest {
    @Test
    void guiSubscriberDoesNotCancelAnotherOwnerOfSameLoad() {
        var key = new ModelRenderTargetRequestKey(new Hash256(new byte[Hash256.SIZE]),
                new ModelContentVersion(1), "player", "default");
        var loads = new AtomicInteger();
        var loaderContext = new AtomicReference<TaskContext>();
        var pending = new CompletableFuture<ModelRenderTarget>();
        try (var cache = new ModelRenderTargetCache(new ModelFailureRegistry());
             var guiScope = TaskScope.create(Runnable::run);
             var bindingScope = TaskScope.create(Runnable::run)) {
            var gui = cache.acquire(guiScope, key, ModelRenderTargetCache.Retention.IDLE,
                    context -> {
                        loads.incrementAndGet();
                        loaderContext.set(context);
                        return pending;
                    });
            var binding = cache.acquire(bindingScope, key, ModelRenderTargetCache.Retention.IDLE,
                    context -> {
                        loads.incrementAndGet();
                        return pending;
                    });

            assertEquals(1, loads.get());
            guiScope.close();
            assertThrows(CancellationException.class, gui::join);
            assertFalse(loaderContext.get().cancelled());

            bindingScope.close();
            assertThrows(CancellationException.class, binding::join);
            assertTrue(loaderContext.get().cancelled());
            assertEquals(0, cache.loadingCount());
        }
    }

    @Test
    void realFailureIsFrozenUntilContentVersionChanges() {
        var hash = new Hash256(new byte[Hash256.SIZE]);
        var first = new ModelRenderTargetRequestKey(hash, new ModelContentVersion(1),
                "player", "default");
        var second = new ModelRenderTargetRequestKey(hash, new ModelContentVersion(2),
                "player", "default");
        var loads = new AtomicInteger();
        try (var cache = new ModelRenderTargetCache(new ModelFailureRegistry());
             var scope = TaskScope.create(Runnable::run)) {
            ModelRenderTargetCache.Loader loader = ignored -> {
                loads.incrementAndGet();
                return CompletableFuture.failedFuture(new IOException("broken"));
            };

            assertThrows(CompletionException.class, () -> cache.acquire(scope, first,
                    ModelRenderTargetCache.Retention.IDLE, loader).join());
            assertThrows(CompletionException.class, () -> cache.acquire(scope, first,
                    ModelRenderTargetCache.Retention.IDLE, loader).join());
            assertEquals(1, loads.get());

            assertThrows(CompletionException.class, () -> cache.acquire(scope, second,
                    ModelRenderTargetCache.Retention.IDLE, loader).join());
            assertEquals(2, loads.get());
        }
    }
}
