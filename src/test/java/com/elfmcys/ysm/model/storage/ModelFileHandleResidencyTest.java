package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.format.container.AssetContainerConstant;
import com.elfmcys.ysm.format.parser.ModelParser;
import com.elfmcys.ysm.format.schema.file.FileChunkDataSource;
import com.elfmcys.ysm.format.vfs.Directory;
import com.elfmcys.ysm.model.catalog.CatalogModelLocation;
import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import com.elfmcys.ysm.model.catalog.DefaultLazyChunkPlan;
import com.elfmcys.ysm.model.domain.ModelPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelFileHandleResidencyTest {
    @TempDir
    static Path fixtureTemp;

    @TempDir
    Path temp;

    private static Path fixture;

    @BeforeAll
    static void createFixture() throws Exception {
        var manifest = ModelFileHandleResidencyTest.class.getResource(
                "/assets/ysm/builtin/default/ysm.json");
        var source = Path.of(java.util.Objects.requireNonNull(manifest).toURI()).getParent();
        try (var vfs = new Directory(source)) {
            fixture = ModelParser.parseBuiltinDefault(
                    vfs, Files.createDirectories(fixtureTemp.resolve("model")));
        }
    }

    @Test
    void capturedTextureSourceUsesResidentChunksAfterTemporaryFileIsDeleted()
            throws Exception {
        var file = copyFixture("resident-default.mxc");
        var handle = ModelFileHandle.openResidentDefault(file, location("default"));
        var capturedChunks = handle.chunks();
        assertInstanceOf(ResidentHandoffChunkDataSource.class, capturedChunks);
        assertFalse(handle.residentOnly());

        var target = handle.view().getRenderTargets().stream()
                .filter(value -> !value.getTextureNames().isEmpty())
                .findFirst()
                .orElseThrow();
        var textureName = target.getTextureNames().iterator().next();
        var capturedTexture = target.textureSources(capturedChunks, textureName).uv();
        var retainedTypes = DefaultLazyChunkPlan.collect(handle.view());
        var discardedChunk = handle.view().getFileView().getAssetView()
                .getChunkTable().values().stream()
                .filter(chunk -> !retainedTypes.contains(chunk.type()))
                .filter(chunk -> !chunk.type().equals(
                        AssetContainerConstant.VERIFICATION_CHUNK_TYPE))
                .findFirst()
                .orElseThrow();

        handle.retainChunksInMemory(retainedTypes);
        assertSame(capturedChunks, handle.chunks());
        assertTrue(handle.residentOnly());
        assertTrue(handle.descriptor().hasEncodedRepresentation());
        handle.discardEncodedRepresentation();
        assertFalse(handle.descriptor().hasEncodedRepresentation());
        Files.delete(file);

        try (var image = capturedTexture.open()) {
            assertTrue(image.data().size() > 0);
        }
        assertThrows(FileNotFoundException.class, () ->
                capturedChunks.readPayloadBytes(discardedChunk));
    }

    @Test
    void ordinaryFileBackedHandlesCannotEnterDefaultResidencyLifecycle()
            throws Exception {
        var direct = ModelFileHandle.openDirect(
                copyFixture("direct.mxc"), location("direct"));
        var converted = ModelFileHandle.openConverted(
                copyFixture("converted.mxc"), location("converted"));

        for (var handle : new ModelFileHandle[]{direct, converted}) {
            assertInstanceOf(FileChunkDataSource.class, handle.chunks());
            assertFalse(handle.residentOnly());
            assertThrows(IllegalStateException.class, () ->
                    handle.retainChunksInMemory(Set.of()));
            assertThrows(IllegalStateException.class,
                    handle::discardEncodedRepresentation);
        }
    }

    private Path copyFixture(String name) throws Exception {
        return Files.copy(fixture, temp.resolve(name));
    }

    private static CatalogModelLocation location(String path) {
        return new CatalogModelLocation(
                CatalogRootKind.BUILTIN, new ModelPath(path));
    }
}
