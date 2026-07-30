package com.elfmcys.ysm.network.message.model;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.network.NetworkPayload;
import com.elfmcys.ysm.network.protocol.PeerLimits;
import com.elfmcys.ysm.network.protocol.ProtocolUuid;
import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;
import com.elfmcys.ysm.testutil.LogCapture;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelTransferSenderTest {
    @Test
    void obeysPeerCreditWindowsForOneTwoAndEightActiveTransfers() throws Exception {
        for (var limit : List.of(1, 2, 8)) {
            verifyWindow(limit);
        }
    }

    @Test
    void sendFailureReturnsCreditAndStartsTheNextTransfer() throws Exception {
        var executor = new ScheduledThreadPoolExecutor(1);
        var sender = new ModelTransferSender(executor, () -> Long.MAX_VALUE);
        var playerId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var calls = new AtomicInteger();
        var sent = new CopyOnWriteArrayList<TransferIdentity>();
        Consumer<NetworkPayload<AssetTransferV0.AssetFragment>> output = payload -> {
            try (payload) {
                if (calls.getAndIncrement() == 0) {
                    throw new RuntimeException(new IOException("test send failure"));
                }
                sent.add(identity(payload.protobuf()));
            }
        };
        try (sender; var payload = payload()) {
            var first = sender.send(playerId, limits(1), () -> true, output,
                    sessionId, 0, TransferKind.CATALOG_FULL, payload);
            var second = sender.send(playerId, limits(1), () -> true, output,
                    sessionId, 1, TransferKind.CATALOG_FULL, payload);

            await(() -> first.completion().isDone() && sent.size() == 1);
            assertThrows(CompletionException.class, first.completion()::join);
            assertFalse(second.completion().isDone());

            sender.release(playerId, sent.get(0).release());
            second.completion().join();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void sessionCancellationCleansQueuedAndActiveTransfers() throws Exception {
        var executor = new ScheduledThreadPoolExecutor(1);
        var sender = new ModelTransferSender(executor, () -> Long.MAX_VALUE);
        var playerId = UUID.randomUUID();
        var cancelledSession = UUID.randomUUID();
        var survivingSession = UUID.randomUUID();
        var sent = new CopyOnWriteArrayList<TransferIdentity>();
        Consumer<NetworkPayload<AssetTransferV0.AssetFragment>> output = payload -> {
            try (payload) {
                sent.add(identity(payload.protobuf()));
            }
        };
        try (sender; var payload = payload()) {
            var active = sender.send(playerId, limits(1), () -> true, output,
                    cancelledSession, 0, TransferKind.CATALOG_FULL, payload);
            var queued = sender.send(playerId, limits(1), () -> true, output,
                    cancelledSession, 1, TransferKind.CATALOG_FULL, payload);
            var survivor = sender.send(playerId, limits(1), () -> true, output,
                    survivingSession, 2, TransferKind.CATALOG_FULL, payload);

            await(() -> sent.size() == 1);
            sender.cancel(playerId, cancelledSession);
            await(() -> sent.size() == 2);

            assertThrows(CancellationException.class, active.completion()::join);
            assertThrows(CancellationException.class, queued.completion()::join);
            assertEquals(survivingSession, sent.get(1).requestId());
            assertFalse(survivor.completion().isDone());

            sender.release(playerId, sent.get(1).release());
            survivor.completion().join();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void duplicateAndUnknownReleasesAreIgnoredAndRateLimited() throws Exception {
        var executor = new ScheduledThreadPoolExecutor(1);
        var sender = new ModelTransferSender(executor, () -> Long.MAX_VALUE);
        var playerId = UUID.randomUUID();
        var sent = new CopyOnWriteArrayList<TransferIdentity>();
        Consumer<NetworkPayload<AssetTransferV0.AssetFragment>> output = payload -> {
            try (payload) {
                sent.add(identity(payload.protobuf()));
            }
        };
        try (sender; var payload = payload(); var logs = new LogCapture(YesSteveModel.LOGGER)) {
            var handle = sender.send(playerId, limits(1), () -> true, output,
                    UUID.randomUUID(), 0, TransferKind.CATALOG_FULL, payload);
            await(() -> sent.size() == 1);
            var release = sent.get(0).release();

            sender.release(playerId, release);
            sender.release(playerId, release);
            sender.release(playerId, release);

            handle.completion().join();
            var levels = logs.events().stream()
                    .filter(event -> event.getMessage().getFormattedMessage()
                            .contains("unknown or duplicate asset transfer release"))
                    .map(org.apache.logging.log4j.core.LogEvent::getLevel)
                    .toList();
            assertEquals(List.of(Level.WARN, Level.DEBUG), levels);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void disconnectCancelsQueuedAndActiveTransfers() throws Exception {
        var executor = new ScheduledThreadPoolExecutor(1);
        var sender = new ModelTransferSender(executor, () -> Long.MAX_VALUE);
        var playerId = UUID.randomUUID();
        var sent = new CopyOnWriteArrayList<TransferIdentity>();
        Consumer<NetworkPayload<AssetTransferV0.AssetFragment>> output = payload -> {
            try (payload) {
                sent.add(identity(payload.protobuf()));
            }
        };
        var handles = new ArrayList<ModelTransferSender.TransferHandle>();
        try (sender; var payload = payload()) {
            for (var resource = 0; resource < 3; resource++) {
                handles.add(sender.send(playerId, limits(2), () -> true, output,
                        UUID.randomUUID(), resource, TransferKind.CATALOG_FULL, payload));
            }
            await(() -> sent.size() == 2);

            sender.playerDisconnected(playerId);

            for (var handle : handles) {
                assertThrows(CancellationException.class, handle.completion()::join);
            }
            assertEquals(2, sent.size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void receiverCanReleaseARejectedTransferBeforeItsFinalFragment() throws Exception {
        var executor = new ScheduledThreadPoolExecutor(1);
        var sender = new ModelTransferSender(executor, () -> Long.MAX_VALUE);
        var playerId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var sentResources = new CopyOnWriteArrayList<Integer>();
        Consumer<NetworkPayload<AssetTransferV0.AssetFragment>> output = payload -> {
            try (payload) {
                var fragment = payload.protobuf();
                sentResources.add(fragment.getResourceIndex());
                if (fragment.getResourceIndex() == 0) {
                    sender.release(playerId, identity(fragment).release());
                }
            }
        };
        try (sender; var payload = PreparedTransfer.catalog(
                2048, ArrayBuffer.move(new byte[2048]), false)) {
            var rejected = sender.send(playerId, limits(1), () -> true, output,
                    sessionId, 0, TransferKind.CATALOG_FULL, payload);
            var next = sender.send(playerId, limits(1), () -> true, output,
                    sessionId, 1, TransferKind.CATALOG_FULL, payload);

            await(() -> rejected.completion().isDone() && sentResources.contains(1));
            assertEquals(1, sentResources.stream().filter(resource -> resource == 0).count());
            assertFalse(next.completion().isDone());
        } finally {
            executor.shutdownNow();
        }
    }

    private static void verifyWindow(int limit) throws Exception {
        var executor = new ScheduledThreadPoolExecutor(1);
        var sender = new ModelTransferSender(executor, () -> Long.MAX_VALUE);
        var playerId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var sent = new CopyOnWriteArrayList<TransferIdentity>();
        Consumer<NetworkPayload<AssetTransferV0.AssetFragment>> output = payload -> {
            try (payload) {
                sent.add(identity(payload.protobuf()));
            }
        };
        var handles = new ArrayList<ModelTransferSender.TransferHandle>();
        try (sender; var payload = payload()) {
            for (var resource = 0; resource < 10; resource++) {
                handles.add(sender.send(playerId, limits(limit), () -> true, output,
                        sessionId, resource, TransferKind.CATALOG_FULL, payload));
            }

            await(() -> sent.size() >= limit);
            assertEquals(limit, sent.size());
            assertTrue(handles.stream().noneMatch(handle -> handle.completion().isDone()));

            for (var released = 0; released < 10; released++) {
                var transfer = sent.get(released);
                sender.release(playerId, transfer.release());
                handles.get(transfer.resourceIndex()).completion().join();
                var expectedSent = Math.min(10, limit + released + 1);
                await(() -> sent.size() >= expectedSent);
                assertEquals(expectedSent, sent.size());
            }
            assertTrue(handles.stream().allMatch(handle -> handle.completion().isDone()));
        } finally {
            executor.shutdownNow();
        }
    }

    private static PreparedTransfer payload() {
        return PreparedTransfer.catalog(1, ArrayBuffer.move(new byte[]{1}), false);
    }

    private static PeerLimits limits(int maxInFlightTransfers) {
        return new PeerLimits(8192, 1024, maxInFlightTransfers, 8192, 8192);
    }

    private static TransferIdentity identity(AssetTransferV0.AssetFragment fragment) {
        return new TransferIdentity(ProtocolUuid.get(fragment.getTransferId()),
                ProtocolUuid.get(fragment.getRequestId()), fragment.getResourceIndex());
    }

    private static void await(BooleanSupplier condition) throws Exception {
        var deadline = System.nanoTime() + java.time.Duration.ofSeconds(2).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertTrue(condition.getAsBoolean(), "Timed out waiting for transfer state");
    }

    private record TransferIdentity(UUID transferId, UUID requestId, int resourceIndex) {
        private AssetTransferV0.AssetTransferRelease release() {
            return AssetTransferMessages.release(transferId, requestId, resourceIndex);
        }
    }
}
