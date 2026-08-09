package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.format.parser.ModelParser;
import com.elfmcys.ysm.format.vfs.Directory;
import com.elfmcys.ysm.model.importer.LegacyImporter;
import com.elfmcys.ysm.model.importer.RawModelImporter;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.storage.AtomicSharedCache;
import com.elfmcys.ysm.model.storage.ConversionProfileId;
import com.elfmcys.ysm.model.storage.ConvertedObjectStore;
import com.elfmcys.ysm.model.storage.ConvertedSourceIndexStore;
import com.elfmcys.ysm.model.storage.SharedCachePaths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogReconcilerTest {
    @TempDir
    static Path fixtureTemp;

    @TempDir
    Path temp;

    private static Path fixture;

    @BeforeAll
    static void createFixture() throws Exception {
        var manifest = CatalogReconcilerTest.class.getResource(
                "/assets/ysm/builtin/default/ysm.json");
        var source = Path.of(java.util.Objects.requireNonNull(manifest).toURI()).getParent();
        try (var vfs = new Directory(source)) {
            fixture = ModelParser.parse(vfs, Files.createDirectories(fixtureTemp.resolve("model")),
                    com.elfmcys.ysm.format.parser.DefaultAnimationFilter.keepAll());
        }
    }

    @Test
    void completeInventoryPublishesDirectModelAndProvesItsRemoval() throws Exception {
        var root = Files.createDirectories(temp.resolve("custom"));
        var direct = root.resolve("model.mxc");
        Files.copy(fixture, direct);
        var reconciler = reconciler(root);

        var loaded = reconciler.reconcile(ReloadableCatalogSnapshot.unready(),
                ReloadRequest.startup());
        assertTrue(loaded.snapshot().ready());
        assertEquals(1, loaded.snapshot().models().size());

        Files.delete(direct);
        var removed = reconciler.reconcile(loaded.snapshot(), new ReloadRequest(
                Set.of(ReloadReason.WATCH_EVENT), AuditLevel.INCREMENTAL,
                Set.of(direct), Set.of()));
        assertTrue(removed.snapshot().models().isEmpty());
        assertTrue(removed.snapshot().sources().isEmpty());
        assertEquals(2, removed.snapshot().reloadGeneration());
    }

    @Test
    void invalidDirectSourceIsPerSourceRejectionNotGlobalFailure() throws Exception {
        var root = Files.createDirectories(temp.resolve("custom-invalid"));
        Files.write(root.resolve("broken.mxc"),
                com.elfmcys.ysm.format.container.AssetContainerConstant.HEAD);

        var result = reconciler(root).reconcile(ReloadableCatalogSnapshot.unready(),
                ReloadRequest.startup());

        assertTrue(result.snapshot().ready());
        assertTrue(result.snapshot().models().isEmpty());
        assertEquals(1, result.snapshot().report().errorCount());
        assertFalse(result.snapshot().sources().isEmpty());
    }

    private CatalogReconciler reconciler(Path root) {
        var paths = new SharedCachePaths(temp.resolve("cache"));
        var cache = new AtomicSharedCache(paths);
        var profile = ConversionProfileId.from(
                com.elfmcys.ysm.model.storage.ConversionProfileInputs.production(
                        new Hash256(new byte[Hash256.SIZE])));
        var resolver = new ModelSourceResolver(new RawModelImporter(
                new LegacyImporter(),
                com.elfmcys.ysm.format.parser.DefaultAnimationFilter.keepAll()),
                new ConvertedSourceIndexStore(paths, cache, profile),
                new ConvertedObjectStore(paths, cache, profile));
        return new CatalogReconciler(List.of(
                new ModelCatalogSource(CatalogRootKind.CUSTOM, root, false)),
                resolver, Set.of());
    }
}
