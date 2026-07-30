package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.buffer.BufferType;
import com.elfmcys.ysm.format.container.AssetContainerConstant;
import com.elfmcys.ysm.format.schema.file.ChunkDataSource;
import com.elfmcys.ysm.format.schema.file.FileChunkDataSource;
import com.elfmcys.ysm.format.schema.model.ModelFileView;
import com.elfmcys.ysm.model.catalog.CatalogModelLocation;
import com.elfmcys.ysm.model.domain.ModelDescriptor;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class ModelFileHandle implements ModelDataHandle {
    private final Path file;
    private final ModelBackingIdentity backingIdentity;
    private final ChunkDataSource chunks;
    private volatile ModelDescriptor descriptor;
    private final CatalogModelLocation location;

    private ModelFileHandle(Path file, CatalogModelLocation location, ModelDescriptor descriptor,
                            ModelBackingIdentity backingIdentity) {
        this.file = file;
        this.location = location;
        this.descriptor = descriptor;
        this.backingIdentity = backingIdentity;
        var fileChunks = new FileChunkDataSource(file, backingIdentity);
        this.chunks = backingIdentity.kind() == ModelBackingIdentity.Kind.RESIDENT_DEFAULT
                ? new ResidentHandoffChunkDataSource(fileChunks)
                : fileChunks;
    }

    public static ModelFileHandle openDirect(Path file, CatalogModelLocation location) throws IOException {
        file = file.toAbsolutePath().normalize();
        var before = ModelBackingIdentity.DirectContainer.capture(file);
        var descriptor = readDescriptor(file);
        verifyChunks(file, descriptor, before);
        var after = ModelBackingIdentity.DirectContainer.capture(file);
        if (!before.equals(after)) {
            throw new IOException("Model file changed while it was being verified: " + file);
        }
        return new ModelFileHandle(file, location, descriptor, after);
    }

    public static ModelFileHandle openConverted(Path file, CatalogModelLocation location) throws IOException {
        file = file.toAbsolutePath().normalize();
        var descriptor = readDescriptor(file);
        return new ModelFileHandle(file, location, descriptor,
                new ModelBackingIdentity.ConvertedObject(file, descriptor.modelHash()));
    }

    public static ModelFileHandle openResidentDefault(Path file, CatalogModelLocation location)
            throws IOException {
        file = file.toAbsolutePath().normalize();
        var descriptor = readDescriptor(file);
        return new ModelFileHandle(file, location, descriptor,
                new ModelBackingIdentity.ResidentDefault(descriptor.modelHash()));
    }

    private static ModelDescriptor readDescriptor(Path file) throws IOException {
        try (var channel = FileChannel.open(file, StandardOpenOption.READ)) {
            var view = new ModelFileView(channel);
            var preamble = readPreamble(channel, view.getFileView().getAssetView().getContainerPreambleSize());
            var manifest = view.getManifestBytes();
            return new ModelDescriptor(
                    view.getModelHash(),
                    ModelHashing.descriptorHash(preamble, manifest),
                    preamble,
                    manifest,
                    view);
        }
    }

    private static void verifyChunks(Path file, ModelDescriptor descriptor,
                                     ModelBackingIdentity backingIdentity) throws IOException {
        var source = new FileChunkDataSource(file, backingIdentity);
        var chunks = descriptor.view().getFileView().getAssetView().getChunkTable().values().stream()
                .sorted(java.util.Comparator.comparingInt(
                        com.elfmcys.ysm.format.container.AssetContainerView.ChunkInfo::offset))
                .toList();
        for (var chunk : chunks) {
            if (chunk.type().equals(AssetContainerConstant.VERIFICATION_CHUNK_TYPE)) {
                continue;
            }
            try (var ignored = source.readStoredVerified(chunk, BufferType.ARRAY)) {
                // Reading is the validation.
            }
        }
    }

    private static byte[] readPreamble(FileChannel channel, int size) throws IOException {
        var bytes = new byte[size];
        var target = ByteBuffer.wrap(bytes);
        channel.position(0);
        while (target.hasRemaining()) {
            if (channel.read(target) < 0) {
                throw new EOFException("Model file changed while reading its container preamble");
            }
        }
        return bytes;
    }

    public Path file() {
        return file;
    }

    public ModelBackingIdentity backingIdentity() {
        return backingIdentity;
    }

    public ModelFileView view() {
        return descriptor.view();
    }

    public ChunkDataSource chunks() {
        return chunks;
    }

    public synchronized void retainChunksInMemory(java.util.Set<String> chunkTypes)
            throws IOException {
        if (!(chunks instanceof ResidentHandoffChunkDataSource residentChunks)) {
            throw new IllegalStateException(
                    "Only the builtin default model can retain lazy chunks in memory");
        }
        residentChunks.promote(
                descriptor.view().getFileView().getAssetView(), chunkTypes);
    }

    /** Removes container bytes that are only needed for transfer/export after the file backing is gone. */
    public synchronized void discardEncodedRepresentation() {
        if (!(chunks instanceof ResidentHandoffChunkDataSource residentChunks)
                || !residentChunks.residentOnly()) {
            throw new IllegalStateException("Cannot discard container bytes before lazy chunks are resident");
        }
        descriptor.view().discardManifestBytes();
        descriptor = descriptor.withoutEncodedRepresentation();
    }

    public boolean residentOnly() {
        return chunks instanceof ResidentHandoffChunkDataSource residentChunks
                && residentChunks.residentOnly();
    }

    public ModelDescriptor descriptor() {
        return descriptor;
    }

    public CatalogModelLocation location() {
        return location;
    }
}
