package com.elfmcys.ysm.client.model.internal.transfer;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.testutil.LogCapture;
import com.elfmcys.ysm.model.cache.ScopedIdleValueCache;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.source.CatalogCursor;
import com.elfmcys.ysm.model.source.ModelAssetSelector;
import com.elfmcys.ysm.model.source.ModelAssetSubject;
import com.elfmcys.ysm.model.source.SourceId;
import com.elfmcys.ysm.network.NetworkPayload;
import com.elfmcys.ysm.network.message.model.AssetTransferMessages;
import com.elfmcys.ysm.network.protocol.ProtocolUuid;
import com.elfmcys.ysm.proto.network.model.ModelAssetsProto;
import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;
import com.elfmcys.ysm.task.TaskScope;
import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.Level;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientTransferManagerBatchTest {
    @Test
    void rateLimitsProtocolWarningsUntilReconnect() {
        var manager = new ClientTransferManager(Runnable::run, (kind, data) -> { }, ignored -> { });
        manager.connect();
        try (var logs = new LogCapture(YesSteveModel.LOGGER)) {
            var invalid = AssetTransferV0.ModelAssetBatchFailure.newInstance();

            manager.requestFailed(invalid);
            manager.requestFailed(invalid);
            manager.disconnect();
            manager.connect();
            manager.requestFailed(invalid);

            var levels = logs.events().stream()
                    .filter(event -> event.getMessage().getFormattedMessage()
                            .contains("invalid identity"))
                    .map(org.apache.logging.log4j.core.LogEvent::getLevel)
                    .toList();
            assertEquals(java.util.List.of(Level.WARN, Level.DEBUG, Level.WARN), levels);
        } finally {
            manager.disconnect();
        }
    }

    @Test
    void sendsOneRequestAndOneCancellationForMultipleResources() {
        var outbound = new ArrayList<Object>();
        var manager = new ClientTransferManager(Runnable::run, (kind, data) -> { }, outbound::add);
        manager.connect();
        try (var scope = TaskScope.create(Runnable::run)) {
            var batch = manager.openBatch(cursor(42));
            var first = requestPack(batch, scope, "first", (byte) 1,
                    CompletableFuture.completedFuture(true));
            var second = requestPack(batch, scope, "second", (byte) 2,
                    CompletableFuture.completedFuture(true));

            batch.submit();

            assertEquals(1, outbound.size());
            var request = assertInstanceOf(AssetTransferV0.ModelAssetBatchRequest.class, outbound.get(0));
            assertEquals(42, request.getCursor().getRevision());
            assertEquals(2, request.getResources().length());

            scope.close();

            assertThrows(CancellationException.class, first::join);
            assertThrows(CancellationException.class, second::join);
            assertEquals(2, outbound.size());
            var cancel = assertInstanceOf(AssetTransferV0.ModelAssetBatchCancel.class, outbound.get(1));
            assertEquals(ProtocolUuid.get(request.getRequestId()), ProtocolUuid.get(cancel.getRequestId()));
        }
    }

    @Test
    void omitsPreflightHitsFromTheNetworkBatch() {
        var outbound = new ArrayList<Object>();
        var manager = new ClientTransferManager(Runnable::run, (kind, data) -> { }, outbound::add);
        manager.connect();
        try (var scope = TaskScope.create(Runnable::run)) {
            var batch = manager.openBatch(cursor(7));
            var cached = requestPack(batch, scope, "cached", (byte) 3,
                    CompletableFuture.completedFuture(false));
            requestPack(batch, scope, "missing", (byte) 4,
                    CompletableFuture.completedFuture(true));

            batch.submit();

            assertNull(cached.join());
            var request = assertInstanceOf(AssetTransferV0.ModelAssetBatchRequest.class, outbound.get(0));
            assertEquals(1, request.getResources().length());
            assertEquals("missing", request.getResources().get(0).getSubject().getPack().getHierarchy());
        }
    }

    @Test
    void isolatesResourceFailureAndPropagatesSessionFailure() {
        var outbound = new ArrayList<Object>();
        var manager = new ClientTransferManager(Runnable::run, (kind, data) -> { }, outbound::add);
        manager.connect();
        try (var scope = TaskScope.create(Runnable::run)) {
            var batch = manager.openBatch(cursor(11));
            var first = requestPack(batch, scope, "first", (byte) 5,
                    CompletableFuture.completedFuture(true));
            var second = requestPack(batch, scope, "second", (byte) 6,
                    CompletableFuture.completedFuture(true));
            batch.submit();
            var request = assertInstanceOf(AssetTransferV0.ModelAssetBatchRequest.class, outbound.get(0));
            var requestId = ProtocolUuid.get(request.getRequestId());

            manager.requestFailed(AssetTransferMessages.resourceFailure(requestId, 0,
                    AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_NOT_FOUND));

            assertThrows(CompletionException.class, first::join);
            assertFalse(second.isDone());

            manager.requestFailed(AssetTransferMessages.sessionFailure(requestId,
                    AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_STALE_CATALOG));

            assertThrows(CompletionException.class, second::join);
        }
    }

    @Test
    void sharedCacheSubscriberKeepsTheBatchSessionAlive() {
        var outbound = new ArrayList<Object>();
        var manager = new ClientTransferManager(Runnable::run, (kind, data) -> { }, outbound::add);
        manager.connect();
        try (var cache = new ScopedIdleValueCache<String, Object>(
                Duration.ofSeconds(30), ignored -> { });
             var firstScope = TaskScope.create(Runnable::run);
             var secondScope = TaskScope.create(Runnable::run)) {
            var batch = manager.openBatch(cursor(15));
            var first = cache.get(firstScope, "cover", (loaderContext, key) ->
                    requestPack(batch, loaderContext, key, (byte) 7,
                            CompletableFuture.completedFuture(true)).thenApply(value -> new Object()));
            var second = cache.get(secondScope, "cover", (loaderContext, key) ->
                    CompletableFuture.failedFuture(new AssertionError("single-flight loader ran twice")));
            batch.submit();

            firstScope.close();

            assertThrows(CancellationException.class, first::join);
            assertFalse(second.isDone());
            assertEquals(1, outbound.size());

            secondScope.close();

            assertThrows(CancellationException.class, second::join);
            assertEquals(2, outbound.size());
            assertInstanceOf(AssetTransferV0.ModelAssetBatchCancel.class, outbound.get(1));
        }
    }

    @Test
    void releasesCompletedAndRejectedTransfersAtMostOnce() {
        var outbound = new ArrayList<Object>();
        var manager = new ClientTransferManager(Runnable::run,
                (kind, data) -> data.close(), outbound::add);
        manager.connect();
        try {
            var completedId = UUID.randomUUID();
            var completed = catalogFragment(completedId,
                    AssetTransferV0.TransferPayloadEncoding.TRANSFER_PAYLOAD_ENCODING_PROTOBUF);
            try (completed) {
                manager.receive(completed);
            }

            assertEquals(1, releases(outbound).size());
            assertEquals(completedId,
                    ProtocolUuid.get(releases(outbound).get(0).getTransferId()));

            var duplicate = catalogFragment(completedId,
                    AssetTransferV0.TransferPayloadEncoding.TRANSFER_PAYLOAD_ENCODING_PROTOBUF);
            try (duplicate) {
                manager.receive(duplicate);
            }
            assertEquals(1, releases(outbound).size());

            var rejectedId = UUID.randomUUID();
            var rejected = catalogFragment(rejectedId,
                    AssetTransferV0.TransferPayloadEncoding.TRANSFER_PAYLOAD_ENCODING_UNSPECIFIED);
            try (rejected) {
                manager.receive(rejected);
            }
            assertEquals(2, releases(outbound).size());
            assertEquals(rejectedId,
                    ProtocolUuid.get(releases(outbound).get(1).getTransferId()));

            var rejectedDuplicate = catalogFragment(rejectedId,
                    AssetTransferV0.TransferPayloadEncoding.TRANSFER_PAYLOAD_ENCODING_UNSPECIFIED);
            try (rejectedDuplicate) {
                manager.receive(rejectedDuplicate);
            }
            assertEquals(2, releases(outbound).size());
        } finally {
            manager.disconnect();
        }
    }

    @Test
    void resourceCancellationReleasesInboundReservationAndCancelsTheBatch() {
        var outbound = new ArrayList<Object>();
        var manager = new ClientTransferManager(Runnable::run,
                (kind, data) -> data.close(), outbound::add);
        manager.connect();
        try (var scope = TaskScope.create(Runnable::run)) {
            var batch = manager.openBatch(cursor(31));
            var result = requestPack(batch, scope, "cancelled", (byte) 9,
                    CompletableFuture.completedFuture(true));
            batch.submit();
            var request = assertInstanceOf(AssetTransferV0.ModelAssetBatchRequest.class, outbound.get(0));
            var requestId = ProtocolUuid.get(request.getRequestId());
            var transferId = UUID.randomUUID();

            var fragment = incompleteModelFragment(transferId, requestId);
            try (fragment) {
                manager.receive(fragment);
            }
            assertEquals(0, releases(outbound).size());

            scope.close();

            assertThrows(CancellationException.class, result::join);
            assertEquals(1, releases(outbound).size());
            var release = releases(outbound).get(0);
            assertEquals(transferId, ProtocolUuid.get(release.getTransferId()));
            assertEquals(requestId, ProtocolUuid.get(release.getRequestId()));
            assertEquals(0, release.getResourceIndex());
            assertEquals(1, outbound.stream()
                    .filter(AssetTransferV0.ModelAssetBatchCancel.class::isInstance).count());
        } finally {
            manager.disconnect();
        }
    }

    @Test
    void disconnectFreesAllInboundReservationsForTheNextConnection() {
        var completedCatalogs = new AtomicInteger();
        var outbound = new ArrayList<Object>();
        var manager = new ClientTransferManager(Runnable::run, (kind, data) -> {
            completedCatalogs.incrementAndGet();
            data.close();
        }, outbound::add);
        manager.connect();
        for (var index = 0; index < 8; index++) {
            var fragment = incompleteCatalogFragment(UUID.randomUUID());
            try (fragment) {
                manager.receive(fragment);
            }
        }

        manager.disconnect();
        manager.connect();
        try {
            var completed = catalogFragment(UUID.randomUUID(),
                    AssetTransferV0.TransferPayloadEncoding.TRANSFER_PAYLOAD_ENCODING_PROTOBUF);
            try (completed) {
                manager.receive(completed);
            }
            assertEquals(1, completedCatalogs.get());
        } finally {
            manager.disconnect();
        }
    }

    private static CompletableFuture<com.elfmcys.ysm.network.message.model.ReceivedModelAssets>
    requestPack(ClientTransferManager.AssetBatchSession batch,
                com.elfmcys.ysm.task.TaskContext context,
                String hierarchy, byte fill, java.util.concurrent.CompletionStage<Boolean> required) {
        var hash = new byte[32];
        java.util.Arrays.fill(hash, fill);
        return batch.requestPack(context, new ModelAssetSubject.Pack("remote", hierarchy),
                (ModelAssetSelector.PackCover) ModelAssetSelector.packCover(new Hash256(hash)), required);
    }

    private static CatalogCursor cursor(long revision) {
        return new CatalogCursor(new SourceId("game-server"), new byte[16], revision);
    }

    private static NetworkPayload<AssetTransferV0.AssetFragment> catalogFragment(
            UUID transferId, AssetTransferV0.TransferPayloadEncoding encoding) {
        var fragment = AssetTransferV0.AssetFragment.newInstance()
                .setResourceIndex(-1)
                .setKind(AssetTransferV0.TransferKind.TRANSFER_KIND_CATALOG_FULL)
                .setDecodedSize(1)
                .setTransferSize(1)
                .setFragmentOffset(0)
                .setFragmentIndex(0)
                .setFragmentCount(1)
                .setPayloadEncoding(encoding);
        ProtocolUuid.set(fragment.getMutableTransferId(), transferId);
        ProtocolUuid.set(fragment.getMutableRequestId(), new UUID(0, 0));
        fragment.getMutableProtobufFragment().setInternalArray(new byte[]{1});
        return NetworkPayload.protobuf(fragment);
    }

    private static NetworkPayload<AssetTransferV0.AssetFragment> incompleteModelFragment(
            UUID transferId, UUID requestId) {
        var fragment = AssetTransferV0.AssetFragment.newInstance()
                .setResourceIndex(0)
                .setKind(AssetTransferV0.TransferKind.TRANSFER_KIND_MODEL_ASSET)
                .setDecodedSize(2048)
                .setTransferSize(2048)
                .setFragmentOffset(0)
                .setFragmentIndex(0)
                .setFragmentCount(2)
                .setPayloadEncoding(AssetTransferV0.TransferPayloadEncoding
                        .TRANSFER_PAYLOAD_ENCODING_RAW_ATTACHMENTS)
                .setModelAssetManifest(ModelAssetsProto.ModelAssetManifest.newInstance());
        ProtocolUuid.set(fragment.getMutableTransferId(), transferId);
        ProtocolUuid.set(fragment.getMutableRequestId(), requestId);
        return NetworkPayload.withRaw(fragment,
                com.elfmcys.ysm.buffer.ArrayBuffer.move(new byte[1024]));
    }

    private static NetworkPayload<AssetTransferV0.AssetFragment> incompleteCatalogFragment(
            UUID transferId) {
        var fragment = AssetTransferV0.AssetFragment.newInstance()
                .setResourceIndex(-1)
                .setKind(AssetTransferV0.TransferKind.TRANSFER_KIND_CATALOG_FULL)
                .setDecodedSize(2048)
                .setTransferSize(2048)
                .setFragmentOffset(0)
                .setFragmentIndex(0)
                .setFragmentCount(2)
                .setPayloadEncoding(AssetTransferV0.TransferPayloadEncoding
                        .TRANSFER_PAYLOAD_ENCODING_PROTOBUF);
        ProtocolUuid.set(fragment.getMutableTransferId(), transferId);
        ProtocolUuid.set(fragment.getMutableRequestId(), new UUID(0, 0));
        fragment.getMutableProtobufFragment().setInternalArray(new byte[1024]);
        return NetworkPayload.protobuf(fragment);
    }

    private static java.util.List<AssetTransferV0.AssetTransferRelease> releases(
            java.util.List<Object> outbound) {
        return outbound.stream()
                .filter(AssetTransferV0.AssetTransferRelease.class::isInstance)
                .map(AssetTransferV0.AssetTransferRelease.class::cast)
                .toList();
    }
}
