package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.buffer.BufferType;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.format.container.AssetContainerView;
import com.elfmcys.ysm.format.container.ChunkDecoding;
import com.elfmcys.ysm.format.schema.file.ChunkDataSource;
import com.elfmcys.ysm.model.domain.Hash256;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HexFormat;

/** Reads validated encoded chunk representations from the process-shared remote cache. */
public final class RemoteChunkDataSource implements ChunkDataSource {
    private final SharedCachePaths paths;

    public RemoteChunkDataSource(SharedCachePaths paths) {
        this.paths = paths;
    }

    @Override
    public UniBuffer readPayload(AssetContainerView.ChunkInfo chunk,
                                 BufferType bufferType) throws IOException {
        if (!ChunkDecoding.isZstd(chunk)) {
            return readStoredVerified(chunk, bufferType);
        }
        try (var stored = readBuffer(chunk, BufferType.NATIVE)) {
            return ChunkDecoding.decodeZstd(stored, chunk, bufferType);
        }
    }

    @Override
    public UniBuffer readStoredVerified(AssetContainerView.ChunkInfo chunk,
                                        BufferType bufferType) throws IOException {
        var readType = ChunkDecoding.isZstd(chunk) ? BufferType.NATIVE : bufferType;
        try (var stored = readBuffer(chunk, readType)) {
            ChunkDecoding.validateStored(stored, chunk);
            return bufferType == BufferType.NATIVE
                    ? stored.acquireNative()
                    : stored.acquireArray();
        }
    }

    private UniBuffer readBuffer(AssetContainerView.ChunkInfo chunk, BufferType bufferType) throws IOException {
        return readBuffer(file(chunk), chunk.size(), bufferType);
    }

    private static UniBuffer readBuffer(Path file, int expectedSize,
                                        BufferType bufferType) throws IOException {
        if (Files.size(file) != expectedSize) {
            throw new IOException("Remote model chunk has an unexpected encoded size");
        }
        var result = UniBuffer.allocate(expectedSize, bufferType);
        try (var channel = FileChannel.open(file, StandardOpenOption.READ)) {
            var target = result.nio();
            while (target.hasRemaining()) {
                if (channel.read(target) < 0) {
                    throw new IOException("Remote model chunk changed while reading");
                }
            }
            return result;
        } catch (IOException | RuntimeException | Error error) {
            result.close();
            throw error;
        }
    }

    static boolean validateStored(Path file, String encoding, int encodedSize,
                                   int decodedSize, byte[] expectedHash) {
        try (var source = readBuffer(file, encodedSize, BufferType.NATIVE)) {
            ChunkDecoding.validateStored(source, new AssetContainerView.ChunkInfo(
                    "remote-cache", encoding, 0, encodedSize, decodedSize,
                    0, 0, 0, expectedHash));
            return true;
        } catch (IOException | RuntimeException error) {
            return false;
        }
    }

    private Path file(AssetContainerView.ChunkInfo chunk) throws IOException {
        if (chunk.hash() == null || chunk.hash().length != Hash256.SIZE) {
            throw new IOException("Remote chunk has no content hash: " + chunk.type());
        }
        var file = chunkPath(paths, new Hash256(chunk.hash()), chunk.encoding(), chunk.size());
        if (!Files.isRegularFile(file)) {
            throw new FileNotFoundException("Remote model chunk is not cached: " + chunk.type());
        }
        return file;
    }

    static Path chunkPath(SharedCachePaths paths, Hash256 hash, String encoding, int encodedSize) {
        var encodingId = HexFormat.of().formatHex(encoding.getBytes(StandardCharsets.UTF_8));
        return paths.remoteChunks().resolve(hash + "-" + encodedSize + "-" + encodingId + ".chunk");
    }
}
