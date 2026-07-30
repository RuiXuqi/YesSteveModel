package com.elfmcys.ysm.format.schema.file;

import com.elfmcys.ysm.buffer.BufferType;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.format.container.AssetContainerView;
import com.elfmcys.ysm.task.TaskContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public interface ChunkDataSource {
    /** Returns the decoded logical payload, or the stored bytes for direct media encodings. */
    UniBuffer readPayload(AssetContainerView.ChunkInfo chunk,
                          BufferType bufferType) throws IOException;

    /** Returns the exact stored representation after validating its logical content. */
    UniBuffer readStoredVerified(AssetContainerView.ChunkInfo chunk,
                                 BufferType bufferType) throws IOException;

    default byte[] readPayloadBytes(AssetContainerView.ChunkInfo chunk) throws IOException {
        try (var payload = readPayload(chunk, BufferType.ARRAY)) {
            return toByteArray(payload);
        }
    }

    default byte[] readStoredVerifiedBytes(AssetContainerView.ChunkInfo chunk) throws IOException {
        try (var stored = readStoredVerified(chunk, BufferType.ARRAY)) {
            return toByteArray(stored);
        }
    }

    default InputStream openPayloadStream(AssetContainerView.ChunkInfo chunk) throws IOException {
        return new ByteArrayInputStream(readPayloadBytes(chunk));
    }

    default CompletableFuture<UniBuffer> readPayload(TaskContext context,
                                                      AssetContainerView.ChunkInfo chunk,
                                                      BufferType bufferType) {
        return supplyAsync(context, () -> readPayload(chunk, bufferType));
    }

    default CompletableFuture<UniBuffer> readStoredVerified(TaskContext context,
                                                             AssetContainerView.ChunkInfo chunk,
                                                             BufferType bufferType) {
        return supplyAsync(context, () -> readStoredVerified(chunk, bufferType));
    }

    default CompletableFuture<byte[]> readPayloadBytes(TaskContext context,
                                                        AssetContainerView.ChunkInfo chunk) {
        return supplyAsync(context, () -> readPayloadBytes(chunk));
    }

    default CompletableFuture<byte[]> readStoredVerifiedBytes(TaskContext context,
                                                               AssetContainerView.ChunkInfo chunk) {
        return supplyAsync(context, () -> readStoredVerifiedBytes(chunk));
    }

    default CompletableFuture<InputStream> openPayloadStream(TaskContext context,
                                                             AssetContainerView.ChunkInfo chunk) {
        return supplyAsync(context, () -> openPayloadStream(chunk));
    }

    private static <T> CompletableFuture<T> supplyAsync(TaskContext context, Callable<T> supplier) {
        if (context.cancelled()) {
            return CompletableFuture.failedFuture(
                    new CancellationException("Chunk read was cancelled"));
        }
        return CompletableFuture.supplyAsync(() -> {
            if (context.cancelled()) {
                throw new CancellationException("Chunk read was cancelled");
            }
            try {
                return supplier.call();
            } catch (IOException error) {
                throw new UncheckedIOException(error);
            } catch (RuntimeException | Error error) {
                throw error;
            } catch (Exception error) {
                throw new CompletionException(error);
            }
        }, context.executor());
    }

    private static byte[] toByteArray(UniBuffer buffer) {
        var result = new byte[buffer.size()];
        buffer.nio().get(result);
        return result;
    }
}
