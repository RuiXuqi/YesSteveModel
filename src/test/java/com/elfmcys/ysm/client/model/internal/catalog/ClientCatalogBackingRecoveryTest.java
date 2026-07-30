package com.elfmcys.ysm.client.model.internal.catalog;

import com.elfmcys.ysm.client.model.catalog.ClientCatalogSnapshot;
import com.elfmcys.ysm.format.parser.ModelParser;
import com.elfmcys.ysm.format.vfs.Directory;
import com.elfmcys.ysm.model.catalog.CatalogModelLocation;
import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import com.elfmcys.ysm.model.domain.ModelPath;
import com.elfmcys.ysm.model.storage.ModelFileHandle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientCatalogBackingRecoveryTest {
    @TempDir
    Path temp;

    @Test
    void staleFailureIsRejectedBeforeCurrentRecoveryIsDeduplicated() throws Exception {
        var sourceUri = java.util.Objects.requireNonNull(getClass().getResource(
                "/assets/ysm/builtin/default/ysm.json")).toURI();
        final Path file;
        try (var source = new Directory(Path.of(sourceUri).getParent())) {
            file = ModelParser.parse(source, Files.createDirectories(temp.resolve("parsed")),
                    com.elfmcys.ysm.format.parser.DefaultAnimationFilter.keepAll());
        }
        var location = new CatalogModelLocation(
                CatalogRootKind.CUSTOM, new ModelPath("direct.mxc"));
        var handle = ModelFileHandle.openDirect(file, location);
        var hash = handle.descriptor().modelHash();
        var versions = new AtomicLong(1);
        var first = ClientCatalogSnapshot.merge(ClientCatalogSnapshot.empty(), 1,
                Map.of(hash, handle), List.of(), null, Set.of(), versions::getAndIncrement);
        var firstVersion = first.find(hash).orElseThrow().contentVersion();
        var current = ClientCatalogSnapshot.merge(first, 2,
                Map.of(hash, handle), List.of(), null, Set.of(handle.backingIdentity()),
                versions::getAndIncrement);
        var currentVersion = current.find(hash).orElseThrow().contentVersion();
        var tracker = new ClientCatalogManager.BackingRecoveryTracker();

        assertTrue(tracker.begin(current, hash, firstVersion,
                handle.backingIdentity()).isEmpty());
        assertEquals(0, tracker.pendingCount());
        assertTrue(tracker.begin(current, hash, currentVersion,
                handle.backingIdentity()).isPresent());
        assertTrue(tracker.begin(current, hash, currentVersion,
                handle.backingIdentity()).isEmpty());
        assertEquals(1, tracker.pendingCount());
    }
}
