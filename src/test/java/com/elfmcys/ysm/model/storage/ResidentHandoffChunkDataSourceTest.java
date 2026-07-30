package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.buffer.BufferType;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.format.container.AssetContainerReader;
import com.elfmcys.ysm.format.container.AssetContainerView;
import com.elfmcys.ysm.format.container.AssetContainerWriter;
import com.elfmcys.ysm.format.schema.file.ChunkDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResidentHandoffChunkDataSourceTest {
    @TempDir
    Path temp;

    @Test
    void promotionWaitsForActiveFileReadBeforePublishingResidentChunks() throws Exception {
        var fixture = fixture(Map.of("retained", new byte[]{1, 2, 3, 4}));
        var delegate = new ControlledChunkDataSource(fixture.payloads());
        delegate.blockNextRead();
        var source = new ResidentHandoffChunkDataSource(delegate);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var read = executor.submit(() ->
                    source.readStoredVerifiedBytes(fixture.chunk("retained")));
            assertTrue(delegate.awaitBlockedRead());

            var promotionCalled = new CountDownLatch(1);
            var promotion = executor.submit(() -> {
                promotionCalled.countDown();
                source.promote(fixture.view(), Set.of("retained"));
                return null;
            });
            assertTrue(promotionCalled.await(5, TimeUnit.SECONDS));
            assertFalse(promotion.isDone());

            delegate.releaseBlockedRead();
            assertArrayEquals(fixture.payload("retained"), read.get(5, TimeUnit.SECONDS));
            promotion.get(5, TimeUnit.SECONDS);
            assertTrue(source.residentOnly());
            assertEquals(2, delegate.readCount());

            delegate.failAllReads();
            assertArrayEquals(fixture.payload("retained"),
                    source.readStoredVerifiedBytes(fixture.chunk("retained")));
        } finally {
            delegate.releaseBlockedRead();
            executor.shutdownNow();
        }
    }

    @Test
    void failedPromotionLeavesTheCompleteOriginalDelegatePublished() throws Exception {
        var fixture = fixture(Map.of(
                "a", new byte[]{10, 20},
                "b", new byte[]{30, 40}));
        var delegate = new ControlledChunkDataSource(fixture.payloads());
        delegate.failType("b");
        var source = new ResidentHandoffChunkDataSource(delegate);

        var error = assertThrows(IOException.class, () ->
                source.promote(fixture.view(), Set.of("a", "b")));
        assertTrue(error.getMessage().contains("b"));
        assertFalse(source.residentOnly());

        var readsAfterFailure = delegate.readCount();
        assertArrayEquals(fixture.payload("a"),
                source.readStoredVerifiedBytes(fixture.chunk("a")));
        assertEquals(readsAfterFailure + 1, delegate.readCount());

        delegate.clearFailure();
        source.promote(fixture.view(), Set.of("a", "b"));
        assertTrue(source.residentOnly());
        delegate.failAllReads();
        assertArrayEquals(fixture.payload("a"),
                source.readStoredVerifiedBytes(fixture.chunk("a")));
        assertArrayEquals(fixture.payload("b"),
                source.readStoredVerifiedBytes(fixture.chunk("b")));
    }

    private Fixture fixture(Map<String, byte[]> payloads) throws IOException {
        var file = temp.resolve("fixture-" + System.nanoTime() + ".mxc");
        try (var writer = new AssetContainerWriter();
             var output = FileChannel.open(file, StandardOpenOption.CREATE_NEW,
                     StandardOpenOption.WRITE)) {
            writer.setSchema("test/resident-handoff");
            for (var entry : payloads.entrySet()) {
                try (var data = ArrayBuffer.move(entry.getValue().clone())) {
                    writer.addChunk(entry.getKey(), "", 0,
                            0, 0, data, 0);
                }
            }
            writer.write(output);
        }
        final AssetContainerView view;
        try (var input = FileChannel.open(file, StandardOpenOption.READ)) {
            view = AssetContainerReader.read(input);
        }
        return new Fixture(view, payloads);
    }

    private record Fixture(AssetContainerView view, Map<String, byte[]> payloads) {
        private Fixture {
            var copies = new LinkedHashMap<String, byte[]>();
            payloads.forEach((type, bytes) -> copies.put(type, bytes.clone()));
            payloads = Map.copyOf(copies);
        }

        private AssetContainerView.ChunkInfo chunk(String type) {
            var chunk = view.getChunkInfo(type);
            if (chunk == null) {
                throw new AssertionError("Missing fixture chunk: " + type);
            }
            return chunk;
        }

        private byte[] payload(String type) {
            return payloads.get(type).clone();
        }
    }

    private static final class ControlledChunkDataSource implements ChunkDataSource {
        private final Map<String, byte[]> payloads;
        private final AtomicInteger readCount = new AtomicInteger();
        private final AtomicBoolean blockNextRead = new AtomicBoolean();
        private final CountDownLatch blockedRead = new CountDownLatch(1);
        private final CountDownLatch releaseRead = new CountDownLatch(1);
        private volatile String failedType;
        private volatile boolean failAll;

        private ControlledChunkDataSource(Map<String, byte[]> payloads) {
            this.payloads = payloads;
        }

        @Override
        public UniBuffer readPayload(AssetContainerView.ChunkInfo chunk,
                                     BufferType bufferType) throws IOException {
            return readStoredVerified(chunk, bufferType);
        }

        @Override
        public UniBuffer readStoredVerified(AssetContainerView.ChunkInfo chunk,
                                            BufferType bufferType) throws IOException {
            readCount.incrementAndGet();
            if (blockNextRead.compareAndSet(true, false)) {
                blockedRead.countDown();
                try {
                    if (!releaseRead.await(5, TimeUnit.SECONDS)) {
                        throw new IOException("Timed out waiting to release blocked read");
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting to release blocked read", error);
                }
            }
            if (failAll || chunk.type().equals(failedType)) {
                throw new IOException("Controlled read failure: " + chunk.type());
            }
            var bytes = payloads.get(chunk.type());
            if (bytes == null) {
                throw new FileNotFoundException(chunk.type());
            }
            var array = ArrayBuffer.move(bytes.clone());
            if (bufferType == BufferType.ARRAY) {
                return array;
            }
            try (array) {
                return array.acquireNative();
            }
        }

        private void blockNextRead() {
            blockNextRead.set(true);
        }

        private boolean awaitBlockedRead() throws InterruptedException {
            return blockedRead.await(5, TimeUnit.SECONDS);
        }

        private void releaseBlockedRead() {
            releaseRead.countDown();
        }

        private int readCount() {
            return readCount.get();
        }

        private void failType(String type) {
            failedType = type;
        }

        private void clearFailure() {
            failedType = null;
            failAll = false;
        }

        private void failAllReads() {
            failAll = true;
        }
    }
}
