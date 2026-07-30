package com.elfmcys.ysm.format.schema.file;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.buffer.BufferType;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.format.container.AssetContainerView;
import com.elfmcys.ysm.format.container.ChunkDecoding;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Immutable encoded lazy chunks. Every read returns caller-owned storage. */
public final class ResidentChunkDataSource implements ChunkDataSource {
    private final Map<String, byte[]> chunks;

    private ResidentChunkDataSource(Map<String, byte[]> chunks) {
        this.chunks = Map.copyOf(chunks);
    }

    public static ResidentChunkDataSource copyOf(ChunkDataSource source,
                                                 AssetContainerView view,
                                                 Set<String> retainedTypes)
            throws IOException {
        var result = new LinkedHashMap<String, byte[]>();
        for (var type : retainedTypes.stream().sorted().toList()) {
            var chunk = view.getChunkInfo(type);
            if (chunk == null) {
                throw new FileNotFoundException("Resident chunk is missing: " + type);
            }
            result.put(type, source.readStoredVerifiedBytes(chunk));
        }
        return new ResidentChunkDataSource(result);
    }

    @Override
    public UniBuffer readPayload(AssetContainerView.ChunkInfo chunk,
                                 BufferType bufferType) throws IOException {
        if (!ChunkDecoding.isZstd(chunk)) {
            return readStoredVerified(chunk, bufferType);
        }
        try (var stored = copyStored(chunk, BufferType.NATIVE)) {
            return ChunkDecoding.decodeZstd(stored, chunk, bufferType);
        }
    }

    @Override
    public UniBuffer readStoredVerified(AssetContainerView.ChunkInfo chunk,
                                        BufferType bufferType) throws IOException {
        var readType = ChunkDecoding.isZstd(chunk) ? BufferType.NATIVE : bufferType;
        try (var stored = copyStored(chunk, readType)) {
            ChunkDecoding.validateStored(stored, chunk);
            return bufferType == BufferType.NATIVE
                    ? stored.acquireNative()
                    : stored.acquireArray();
        }
    }

    public Set<String> retainedTypes() {
        return chunks.keySet();
    }

    private byte[] require(AssetContainerView.ChunkInfo chunk)
            throws FileNotFoundException {
        var bytes = chunks.get(chunk.type());
        if (bytes == null) {
            throw new FileNotFoundException(
                    "Chunk was preprocessed and its raw payload was discarded: " + chunk.type());
        }
        return bytes;
    }

    private UniBuffer copyStored(AssetContainerView.ChunkInfo chunk,
                                 BufferType bufferType) throws IOException {
        var bytes = require(chunk);
        if (bytes.length != chunk.size()) {
            throw new IOException("Resident chunk has an unexpected stored size: " + chunk.type());
        }
        var array = ArrayBuffer.move(bytes.clone());
        if (bufferType == BufferType.ARRAY) {
            return array;
        }
        try (array) {
            return array.acquireNative();
        }
    }
}
