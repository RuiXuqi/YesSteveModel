package com.elfmcys.ysm.client.model.catalog;

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
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ClientCatalogSnapshotTest {
    @TempDir
    Path temp;

    @Test
    void reusesOnlyAnUnchangedAndUnforcedBackingVersion() throws Exception {
        var sourceUri = java.util.Objects.requireNonNull(getClass().getResource(
                "/assets/ysm/builtin/default/ysm.json")).toURI();
        var output = Files.createDirectories(temp.resolve("parsed"));
        final Path file;
        try (var source = new Directory(Path.of(sourceUri).getParent())) {
            file = ModelParser.parse(source, output,
                    com.elfmcys.ysm.format.parser.DefaultAnimationFilter.keepAll());
        }
        var location = new CatalogModelLocation(CatalogRootKind.CUSTOM, new ModelPath("direct.mxc"));
        var versions = new AtomicLong(1);

        var firstHandle = ModelFileHandle.openDirect(file, location);
        var first = ClientCatalogSnapshot.merge(ClientCatalogSnapshot.empty(), 1,
                Map.of(firstHandle.descriptor().modelHash(), firstHandle), List.of(), null,
                Set.of(), versions::getAndIncrement);
        var firstEntry = first.find(firstHandle.descriptor().modelHash()).orElseThrow();

        var unchangedHandle = ModelFileHandle.openDirect(file, location);
        var unchanged = ClientCatalogSnapshot.merge(first, 2,
                Map.of(unchangedHandle.descriptor().modelHash(), unchangedHandle), List.of(), null,
                Set.of(), versions::getAndIncrement);
        assertEquals(firstEntry.contentVersion(), unchanged.find(
                firstHandle.descriptor().modelHash()).orElseThrow().contentVersion());

        var forced = ClientCatalogSnapshot.merge(unchanged, 3,
                Map.of(unchangedHandle.descriptor().modelHash(), unchangedHandle), List.of(), null,
                Set.of(unchangedHandle.backingIdentity()), versions::getAndIncrement);
        assertNotEquals(firstEntry.contentVersion(), forced.find(
                firstHandle.descriptor().modelHash()).orElseThrow().contentVersion());

        Files.setLastModifiedTime(file, FileTime.fromMillis(
                Files.getLastModifiedTime(file).toMillis() + 2000));
        var touchedHandle = ModelFileHandle.openDirect(file, location);
        var touched = ClientCatalogSnapshot.merge(forced, 4,
                Map.of(touchedHandle.descriptor().modelHash(), touchedHandle), List.of(), null,
                Set.of(), versions::getAndIncrement);
        assertNotEquals(forced.find(firstHandle.descriptor().modelHash()).orElseThrow().contentVersion(),
                touched.find(firstHandle.descriptor().modelHash()).orElseThrow().contentVersion());
    }
}
