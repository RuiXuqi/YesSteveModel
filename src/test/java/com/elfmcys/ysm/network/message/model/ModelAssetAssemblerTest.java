package com.elfmcys.ysm.network.message.model;

import com.elfmcys.ysm.format.parser.ModelParser;
import com.elfmcys.ysm.format.parser.DefaultAnimationFilter;
import com.elfmcys.ysm.format.schema.file.AssetFileConstant;
import com.elfmcys.ysm.format.vfs.Directory;
import com.elfmcys.ysm.model.catalog.CatalogModelLocation;
import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import com.elfmcys.ysm.model.domain.ModelPath;
import com.elfmcys.ysm.model.source.ModelAssetSelector;
import com.elfmcys.ysm.model.storage.ModelFileHandle;
import com.elfmcys.ysm.task.TaskScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelAssetAssemblerTest {
    @TempDir
    Path temp;

    @Test
    void compressedModelChunkIsTransferredInItsVerifiedStoredRepresentation()
            throws Exception {
        var manifest = getClass().getResource(
                "/assets/ysm/builtin/default/ysm.json");
        var source = Path.of(java.util.Objects.requireNonNull(manifest).toURI()).getParent();
        final Path modelFile;
        try (var vfs = new Directory(source)) {
            modelFile = ModelParser.parse(
                    vfs, Files.createDirectories(temp.resolve("model")),
                    DefaultAnimationFilter.keepAll());
        }
        var handle = ModelFileHandle.openDirect(modelFile, new CatalogModelLocation(
                CatalogRootKind.CUSTOM, new ModelPath("fixture.mxc")));
        var blobId = handle.view().getPlayer().descriptor().getBlobId();
        var chunkName = AssetFileConstant.BLOB_CHUNK_PREFIX + blobId;
        var chunk = handle.view().getFileView().getAssetView().getChunkInfo(chunkName);
        assertEquals("zstd", chunk.encoding());
        var expectedStored = handle.chunks().readStoredVerifiedBytes(chunk);

        var selector = ModelAssetSelector.renderTarget(
                "player", "", EnumSet.of(ModelAssetSelector.RenderTargetComponent.DEFINITION));
        try (var scope = TaskScope.create(Runnable::run);
             var transfer = new ModelAssetAssembler().assemble(scope, handle, selector).join();
             var content = transfer.acquireContent()) {
            assertEquals(1, transfer.manifest().getChunks().length());
            var descriptor = transfer.manifest().getChunks().get(0);
            assertEquals(chunkName, descriptor.getName());
            assertEquals("zstd", descriptor.getEncoding());
            assertEquals(chunk.decodeSize(), descriptor.getDecodedSize());
            assertEquals(expectedStored.length, descriptor.getRawSize());
            assertTrue(expectedStored.length < chunk.decodeSize());
            var actual = new byte[content.size()];
            content.nio().get(actual);
            assertArrayEquals(expectedStored, actual);
        }
    }
}
