package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.domain.ModelPath;
import com.elfmcys.ysm.model.catalog.CatalogRootIdentity;
import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import com.elfmcys.ysm.model.catalog.ModelSourceKey;
import com.elfmcys.ysm.model.catalog.ModelSourceKind;
import com.elfmcys.ysm.model.catalog.RootInventoryState;
import com.elfmcys.ysm.model.catalog.SourceObservation;
import com.elfmcys.ysm.model.catalog.SourceStamp;
import com.elfmcys.ysm.model.catalog.StartupSourceInventory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartupCachePrunerTest {
    @TempDir
    Path temp;

    @Test
    void removesOldProfilesTemporaryEntriesAndIncompleteCurrentPairs() throws Exception {
        var paths = new SharedCachePaths(temp.resolve("cache"));
        var current = new ConversionProfileId(hash(1));
        var currentObjects = Files.createDirectories(paths.convertedObjects(current));
        var completeHash = hash(3);
        var completeObject = currentObjects.resolve(completeHash + ".mxc");
        var completeMetadata = currentObjects.resolve(completeHash + ".meta");
        Files.writeString(completeObject, "object");
        ConvertedObjectMetadata.write(completeMetadata, new ConvertedObjectMetadata(
                new ConvertedObjectKey(current, completeHash), hash(4), Files.size(completeObject)));
        Files.writeString(currentObjects.resolve("orphan.mxc"), "orphan");
        var oldObjects = Files.createDirectories(paths.convertedObjects().resolve(hash(2).toString()));
        Files.writeString(oldObjects.resolve("old.mxc"), "old");
        var temporary = Files.createDirectories(paths.convertedTemporary()).resolve("stale.tmp");
        Files.writeString(temporary, "stale");

        var report = StartupCachePruner.prune(paths, current);

        assertFalse(report.budgetExhausted());
        assertTrue(Files.isRegularFile(completeObject));
        assertTrue(Files.isRegularFile(completeMetadata));
        assertFalse(Files.exists(currentObjects.resolve("orphan.mxc")));
        assertFalse(Files.exists(oldObjects));
        assertFalse(Files.exists(temporary));
    }

    @Test
    void completeStartupInventoryDeletesOnlyOwnedObsoleteSourceIndexes() throws Exception {
        var paths = new SharedCachePaths(temp.resolve("index-cache"));
        var current = new ConversionProfileId(hash(5));
        var store = new ConvertedSourceIndexStore(paths, new AtomicSharedCache(paths), current);
        var root = new CatalogRootIdentity(CatalogRootKind.CUSTOM,
                temp.resolve("models").toAbsolutePath(), "root-key");
        var present = new ModelSourceKey(root, new ModelPath("present"),
                ModelSourceKind.CURRENT_RAW_DIRECTORY);
        var absent = new ModelSourceKey(root, new ModelPath("absent"),
                ModelSourceKind.CURRENT_RAW_DIRECTORY);
        var stamp = new SourceStamp.RawDirectory(hash(6), 1, 10);
        store.write(new ConvertedSourceIndex(present, stamp, current, hash(7), hash(8)));
        store.write(new ConvertedSourceIndex(absent, stamp, current, hash(9), hash(10)));
        var observation = new SourceObservation(present, temp.resolve("models/present"), stamp, 10);
        var complete = new RootInventoryState.Complete(root,
                java.util.Map.of(present, observation), java.util.Map.of());
        var inventory = new StartupSourceInventory(java.util.Map.of(root, complete));

        var report = StartupCachePruner.prune(paths, current, inventory);

        assertTrue(Files.isRegularFile(store.path(present)));
        assertFalse(Files.exists(store.path(absent)));
        assertTrue(report.sourceIndexesDeleted() >= 1);
    }

    private static Hash256 hash(int seed) {
        var bytes = new byte[Hash256.SIZE];
        bytes[0] = (byte) seed;
        return new Hash256(bytes);
    }
}
