package com.elfmcys.ysm.format.schema.file;

import com.elfmcys.ysm.buffer.BufferType;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.format.container.AssetContainerView;
import com.elfmcys.ysm.format.container.InlineChunkReader;
import com.elfmcys.ysm.model.storage.ModelBackingException;
import com.elfmcys.ysm.model.storage.ModelBackingIdentity;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Opens a new channel for every operation, making one ModelFileView safe for concurrent requests. */
public final class FileChunkDataSource implements ChunkDataSource {
    private final Path file;
    private final @Nullable ModelBackingIdentity backingIdentity;

    public FileChunkDataSource(Path file) {
        this(file, null);
    }

    public FileChunkDataSource(Path file, @Nullable ModelBackingIdentity backingIdentity) {
        this.file = file.toAbsolutePath().normalize();
        this.backingIdentity = backingIdentity;
    }

    @Override
    public UniBuffer readPayload(AssetContainerView.ChunkInfo chunk,
                                 BufferType bufferType) throws IOException {
        try (var channel = FileChannel.open(file, StandardOpenOption.READ)) {
            return InlineChunkReader.readPayload(channel, chunk, bufferType);
        } catch (IOException | RuntimeException error) {
            throw readFailure(chunk, error);
        }
    }

    @Override
    public UniBuffer readStoredVerified(AssetContainerView.ChunkInfo chunk,
                                        BufferType bufferType) throws IOException {
        try (var channel = FileChannel.open(file, StandardOpenOption.READ)) {
            return InlineChunkReader.readStoredVerified(channel, chunk, bufferType);
        } catch (IOException | RuntimeException error) {
            throw readFailure(chunk, error);
        }
    }

    private IOException readFailure(AssetContainerView.ChunkInfo chunk, Throwable cause) {
        var message = "Failed to read model chunk file=" + file
                + " type=" + chunk.type()
                + " encoding=" + chunk.encoding()
                + " encodedSize=" + chunk.size()
                + " decodedSize=" + chunk.decodeSize();
        return backingIdentity == null ? new IOException(message, cause)
                : new ModelBackingException(backingIdentity, message, cause);
    }
}
