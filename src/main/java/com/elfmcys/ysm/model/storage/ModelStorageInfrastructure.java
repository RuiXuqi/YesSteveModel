package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.model.catalog.StartupSourceInventory;

import java.io.IOException;
import java.io.UncheckedIOException;

/** The single JVM owner of converted-cache lifetime and profile-scoped stores. */
public final class ModelStorageInfrastructure implements AutoCloseable {
    private final SharedCachePaths paths;
    private final AtomicSharedCache cache;
    private final ConversionProfileId profile;
    private final ConvertedCacheLifecycle lifecycle;
    private final ConvertedObjectStore objects;
    private final ConvertedSourceIndexStore indexes;

    private ModelStorageInfrastructure(SharedCachePaths paths,
                                       ConversionProfileId profile,
                                       StartupSourceInventory inventory) throws IOException {
        this.paths = paths;
        cache = new AtomicSharedCache(paths);
        this.profile = java.util.Objects.requireNonNull(profile, "profile");
        lifecycle = new ConvertedCacheLifecycle(paths);
        var exclusive = lifecycle.tryAcquireStartupExclusive();
        if (exclusive) {
            try {
                var report = StartupCachePruner.prune(paths, profile, inventory);
                YesSteveModel.LOGGER.info(
                        "Pruned converted cache: budgetExhausted={}, oldProfiles={}, sourceIndexes={}, incompleteObjects={}, temporaryEntries={}, errors={}",
                        report.budgetExhausted(), report.oldProfilesDeleted(), report.sourceIndexesDeleted(),
                        report.incompleteObjectsDeleted(),
                        report.temporaryEntriesDeleted(), report.errors());
            } finally {
                lifecycle.releaseExclusive();
            }
        } else {
            YesSteveModel.LOGGER.info(
                    "Skipped converted-cache startup prune because another JVM is reading it");
        }
        lifecycle.acquireShared();
        objects = new ConvertedObjectStore(paths, cache, profile, lifecycle);
        indexes = new ConvertedSourceIndexStore(paths, cache, profile, lifecycle);
    }

    public static ModelStorageInfrastructure openDefault(ConversionProfileId profile) {
        return openDefault(profile, new StartupSourceInventory(java.util.Map.of()));
    }

    public static ModelStorageInfrastructure openDefault(ConversionProfileId profile,
                                                         StartupSourceInventory inventory) {
        try {
            return new ModelStorageInfrastructure(
                    SharedCachePaths.userDefault(), profile, inventory);
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to initialize converted-cache storage", error);
        }
    }

    public SharedCachePaths paths() {
        return paths;
    }

    public AtomicSharedCache cache() {
        return cache;
    }

    public ConversionProfileId profile() {
        return profile;
    }

    public ConvertedObjectStore objects() {
        lifecycle.requireShared();
        return objects;
    }

    public ConvertedSourceIndexStore indexes() {
        lifecycle.requireShared();
        return indexes;
    }

    @Override
    public void close() {
        try {
            lifecycle.close();
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to close converted-cache lifecycle", error);
        }
    }
}
