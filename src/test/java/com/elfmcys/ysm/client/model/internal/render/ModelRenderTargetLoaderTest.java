package com.elfmcys.ysm.client.model.internal.render;

import mixel.asset.model.ModelDataOuterClass;
import mixel.asset.model.data.GeoModelOuterClass;
import com.elfmcys.ysm.task.TaskScope;
import com.elfmcys.ysm.util.ProtoUtil;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class ModelRenderTargetLoaderTest {
    @Test
    void resolvesMainAndArmGeometryFromOneModelData() throws Exception {
        var main = GeoModelOuterClass.GeoModel.newInstance();
        var arm = GeoModelOuterClass.GeoModel.newInstance();
        main.getMutableProperties().setIdentifier("main");
        arm.getMutableProperties().setIdentifier("arm");
        var definition = ModelDataOuterClass.ModelData.newInstance();
        definition.addGeoModels(geometry("main", main));
        definition.addGeoModels(geometry("arm", arm));

        assertArrayEquals(ProtoUtil.serializeToArray(main),
                ProtoUtil.serializeToArray(ModelRenderTargetLoader.geoModel(definition, "main")));
        assertArrayEquals(ProtoUtil.serializeToArray(arm),
                ProtoUtil.serializeToArray(ModelRenderTargetLoader.geoModel(definition, "arm")));
        assertThrows(FileNotFoundException.class,
                () -> ModelRenderTargetLoader.geoModel(definition, "missing"));
    }

    @Test
    void stagedLoadsDoNotStarveSharedFixedWorkerPool() {
        for (var workerCount : new int[]{1, 2, 8}) {
            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                var threadIndex = new AtomicInteger();
                var prefix = "model-loader-" + workerCount + "-";
                var workers = Executors.newFixedThreadPool(workerCount, task ->
                        new Thread(task, prefix + threadIndex.incrementAndGet()));
                var buildThreads = ConcurrentHashMap.<String>newKeySet();
                var buildCalls = new AtomicInteger();
                try (var scope = TaskScope.create(workers)) {
                    var loader = new ModelRenderTargetLoader(workers, (context, input) -> {
                        buildCalls.incrementAndGet();
                        buildThreads.add(Thread.currentThread().getName());
                        return null;
                    });
                    var loads = new CompletableFuture<?>[workerCount * 4 + 1];
                    for (var index = 0; index < loads.length; index++) {
                        var loaded = CompletableFuture.supplyAsync(
                                () -> new ModelRenderTargetLoader.LoadedTarget(null, null, null), workers);
                        loads[index] = loader.buildLoaded(scope, loaded);
                    }

                    CompletableFuture.allOf(loads).join();
                    assertEquals(loads.length, buildCalls.get());
                    assertTrue(buildThreads.stream().allMatch(name -> name.startsWith(prefix)),
                            () -> "Build escaped the owned worker pool: " + buildThreads);
                } finally {
                    workers.shutdownNow();
                }
            });
        }
    }

    @Test
    void cancellationBeforeBuildSkipsBuildStage() {
        var workers = Executors.newSingleThreadExecutor();
        var buildCalls = new AtomicInteger();
        var loaded = new CompletableFuture<ModelRenderTargetLoader.LoadedTarget>();
        try (var scope = TaskScope.create(workers)) {
            var loader = new ModelRenderTargetLoader(workers, (context, input) -> {
                buildCalls.incrementAndGet();
                return null;
            });
            var result = loader.buildLoaded(scope, loaded);

            scope.close();
            loaded.complete(new ModelRenderTargetLoader.LoadedTarget(null, null, null));

            var failure = assertThrows(CompletionException.class, result::join);
            assertInstanceOf(CancellationException.class, failure.getCause());
            assertEquals(0, buildCalls.get());
        } finally {
            workers.shutdownNow();
        }
    }

    private static ModelDataOuterClass.ModelData.GeoModelsEntry geometry(
            String name, GeoModelOuterClass.GeoModel model) throws Exception {
        var entry = ModelDataOuterClass.ModelData.GeoModelsEntry.newInstance().setKey(name);
        entry.getMutableValue().setInternalArray(ProtoUtil.serializeToArray(model));
        return entry;
    }
}
