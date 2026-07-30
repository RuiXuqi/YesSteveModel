package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.buffer.BufferType;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.format.container.AssetContainerView;
import com.elfmcys.ysm.model.domain.ModelDescriptor;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.natives.Blake3;
import com.elfmcys.ysm.natives.Zstd;
import com.elfmcys.ysm.network.message.model.ReceivedModelAssets;
import com.elfmcys.ysm.proto.network.model.ModelAssetsProto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Persistent, process-shared cache for remote metadata and validated encoded chunk representations. */
public final class RemoteModelCache {
    private final SharedCachePaths paths;
    private final AtomicSharedCache cache;
    private final RemoteChunkDataSource chunks;

    public RemoteModelCache(SharedCachePaths paths, AtomicSharedCache cache) {
        this.paths = paths;
        this.cache = cache;
        this.chunks = new RemoteChunkDataSource(paths);
    }

    public RemoteModelHandle storeMetadata(ModelDescriptor descriptor) throws IOException {
        var cachedDescriptor = RemoteModelDescriptorFile.create(descriptor.modelHash(), descriptor.descriptorHash(),
                descriptor.containerPreamble(), descriptor.schemaManifest());
        var target = metadataPath(cachedDescriptor);
        cache.materialize("remote-metadata", cachedDescriptor.modelHash() + ":" + cachedDescriptor.descriptorHash(),
                target, candidate -> RemoteModelDescriptorFile.validate(candidate, cachedDescriptor),
                candidate -> RemoteModelDescriptorFile.write(candidate, cachedDescriptor));
        return open(target);
    }

    public void storeChunks(ReceivedModelAssets assets) throws IOException {
        for (var chunk : assets.manifest().getChunks()) {
            var hash = chunk.getContentHash();
            if (hash.length() != ModelHash.SIZE) {
                throw new IOException("Remote chunk contains no valid content hash: " + chunk.getName());
            }
            var expected = new ModelHash(hash.array(), 0, hash.length());
            try (var data = assets.chunk(chunk)) {
                if (!validateChunk(data, chunk, expected)) {
                    throw new IOException("Downloaded chunk failed content-hash validation: " + chunk.getName());
                }
                var target = RemoteChunkDataSource.chunkPath(
                        paths, expected, chunk.getEncoding(), data.size());
                cache.materialize("remote-chunk", expected.toString(), target,
                        candidate -> validateChunk(candidate, chunk, expected),
                        candidate -> {
                            try (var array = data.acquireArray();
                                 var output = Files.newOutputStream(candidate)) {
                                output.write(array.array(), array.arrayOffset(), array.size());
                            }
                        });
            }
        }
    }

    public void removeMetadata(ModelHash modelHash, ModelHash descriptorHash) throws IOException {
        var target = cache.checkedTarget(metadataPath(modelHash, descriptorHash));
        cache.withKeyLock("remote-metadata", modelHash + ":" + descriptorHash, () -> {
            Files.deleteIfExists(target);
            var parent = target.getParent();
            if (Files.isDirectory(parent)) {
                try (var entries = Files.list(parent)) {
                    if (entries.findAny().isEmpty()) {
                        Files.deleteIfExists(parent);
                    }
                }
            }
            return null;
        });
    }

    private static boolean validateChunk(Path file, ModelAssetsProto.ModelChunk chunk, ModelHash expected) {
        return RemoteChunkDataSource.validateStored(file, chunk.getEncoding(),
                chunk.getRawSize(), chunk.getDecodedSize(), expected.bytes());
    }

    private static boolean validateChunk(Path file, AssetContainerView.ChunkInfo chunk, ModelHash expected) {
        return RemoteChunkDataSource.validateStored(file, chunk.encoding(),
                chunk.size(), chunk.decodeSize(), expected.bytes());
    }

    private static boolean validateChunk(UniBuffer source,
                                         ModelAssetsProto.ModelChunk chunk,
                                         ModelHash expected) {
        return validateChunk(source, chunk.getEncoding(), chunk.getDecodedSize(), expected);
    }

    private static boolean validateChunk(UniBuffer source, String encoding,
                                         int decodedSize, ModelHash expected) {
        if ("zstd".equals(encoding)) {
            try (var ignored = Zstd.decompressAndValidate(
                    source, decodedSize, expected.bytes(), BufferType.NATIVE)) {
                return true;
            } catch (RuntimeException error) {
                return false;
            }
        }
        return Blake3.validateHash(source, expected.bytes());
    }

    public Map<ModelHash, RemoteModelHandle> scan() throws IOException {
        var result = new HashMap<ModelHash, RemoteModelHandle>();
        if (!Files.isDirectory(paths.remoteModels())) {
            return Map.of();
        }
        try (var files = Files.walk(paths.remoteModels())) {
            for (var file : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".meta"))
                    .sorted().toList()) {
                try {
                    var handle = open(file);
                    var previous = result.putIfAbsent(handle.descriptor().modelHash(), handle);
                    if (previous != null && !previous.descriptor().sameRepresentation(handle.descriptor())) {
                        YesSteveModel.LOGGER.debug("Ignoring conflicting remote model metadata {}", file);
                    }
                } catch (IOException | IllegalArgumentException error) {
                    YesSteveModel.LOGGER.debug("Ignoring invalid remote model metadata {}", file, error);
                }
            }
        }
        return Map.copyOf(result);
    }

    public boolean hasChunk(AssetContainerView.ChunkInfo chunk) {
        if (chunk.hash() == null || chunk.hash().length != ModelHash.SIZE) {
            return false;
        }
        try {
            var expected = new ModelHash(chunk.hash());
            var file = RemoteChunkDataSource.chunkPath(paths, expected, chunk.encoding(), chunk.size());
            return Files.isRegularFile(file) && validateChunk(file, chunk, expected);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public RemoteChunkDataSource chunks() {
        return chunks;
    }

    private RemoteModelHandle open(Path metadata) throws IOException {
        var descriptor = RemoteModelDescriptorFile.read(metadata);
        return new RemoteModelHandle(descriptor, chunks);
    }

    private Path metadataPath(ModelDescriptor descriptor) {
        return metadataPath(descriptor.modelHash(), descriptor.descriptorHash());
    }

    private Path metadataPath(ModelHash modelHash, ModelHash descriptorHash) {
        return paths.remoteModels().resolve(modelHash.toString())
                .resolve(descriptorHash + ".meta");
    }

}
