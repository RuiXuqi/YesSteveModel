package com.elfmcys.ysm.client.model.internal.transfer;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.client.model.catalog.ClientCatalogEntry;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.source.CatalogCursor;
import com.elfmcys.ysm.model.source.ModelAssetSelector;
import com.elfmcys.ysm.model.source.ModelAssetSubject;
import com.elfmcys.ysm.model.storage.ModelHashing;
import com.elfmcys.ysm.network.NetworkHandler;
import com.elfmcys.ysm.network.NetworkPayload;
import com.elfmcys.ysm.network.message.model.ModelAssetPlan;
import com.elfmcys.ysm.network.message.model.ModelAssetProtoMapper;
import com.elfmcys.ysm.network.message.model.ReceivedModelAssets;
import com.elfmcys.ysm.network.message.model.TransferKind;
import com.elfmcys.ysm.network.message.model.ZstdPayloadCompression;
import com.elfmcys.ysm.network.protocol.AssetFragmentAssembler;
import com.elfmcys.ysm.network.protocol.AssetTransferBudget;
import com.elfmcys.ysm.network.message.model.AssetTransferMessages;
import com.elfmcys.ysm.network.protocol.ProtocolUuid;
import com.elfmcys.ysm.network.protocol.ProtocolLimits;
import com.elfmcys.ysm.network.protocol.FirstOccurrenceTracker;
import com.elfmcys.ysm.proto.network.model.ModelAssetsProto;
import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;
import com.elfmcys.ysm.task.TaskContext;
import com.elfmcys.ysm.util.Closeable;
import com.elfmcys.ysm.util.ProtoBytes;
import us.hebi.quickbuf.ProtoMessage;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Owns batched client sessions, per-resource fragment assembly and response validation. */
public final class ClientTransferManager {
    private static final UUID NO_SESSION = new UUID(0, 0);
    private static final int NO_RESOURCE = -1;
    private static final int MAX_COMPRESSED_TRANSFER = 128 * 1024 * 1024;
    private static final long TRANSFER_TIMEOUT_NANOS = Duration.ofSeconds(30).toNanos();

    private final Executor workers;
    private final BiConsumer<TransferKind, ArrayBuffer> catalogConsumer;
    private final Consumer<ProtoMessage<?>> networkSender;
    private final ConcurrentHashMap<UUID, InboundTransfer> transfers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, InboundTransfer> processingTransfers = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> releasedTransfers = ConcurrentHashMap.newKeySet();
    private final AssetTransferBudget transferBudget = new AssetTransferBudget(
            ProtocolLimits.MAX_IN_FLIGHT_TRANSFERS, ProtocolLimits.MAX_RESERVED_ASSET_BYTES);
    private final ConcurrentHashMap<UUID, PendingSession> sessions = new ConcurrentHashMap<>();
    private final FirstOccurrenceTracker<ProtocolAnomaly> protocolAnomalies =
            new FirstOccurrenceTracker<>();
    private volatile boolean connected;

    public ClientTransferManager(Executor workers, BiConsumer<TransferKind, ArrayBuffer> catalogConsumer) {
        this(workers, catalogConsumer, NetworkHandler::sendToServer);
    }

    ClientTransferManager(Executor workers, BiConsumer<TransferKind, ArrayBuffer> catalogConsumer,
                          Consumer<ProtoMessage<?>> networkSender) {
        this.workers = workers;
        this.catalogConsumer = catalogConsumer;
        this.networkSender = networkSender;
    }

    public void connect() {
        if (!connected) {
            protocolAnomalies.clear();
            releasedTransfers.clear();
        }
        connected = true;
    }

    public boolean connected() {
        return connected;
    }

    public AssetBatchSession openBatch(CatalogCursor cursor) {
        return new AssetBatchSession(cursor);
    }

    public CompletableFuture<ReceivedModelAssets> request(
            TaskContext context, ClientCatalogEntry entry, ModelAssetSelector selector,
            CatalogCursor cursor) {
        var batch = openBatch(cursor);
        var result = batch.request(context, entry, selector,
                CompletableFuture.completedFuture(true));
        batch.submit();
        return result;
    }

    public void receive(NetworkPayload<AssetTransferV0.AssetFragment> payload) {
        var fragment = payload.protobuf();
        final UUID transferId;
        final UUID sessionId;
        final int resourceIndex;
        try {
            transferId = ProtocolUuid.get(fragment.getTransferId());
            sessionId = ProtocolUuid.get(fragment.getRequestId());
            resourceIndex = fragment.getResourceIndex();
        } catch (RuntimeException error) {
            protocolAnomaly(ProtocolAnomaly.INVALID_FRAGMENT_IDENTITY,
                    "Ignoring model transfer fragment with invalid identity: {}", error.getMessage());
            return;
        }
        if (releasedTransfers.contains(transferId) || processingTransfers.containsKey(transferId)) {
            return;
        }
        PendingSession session = null;
        if (!NO_SESSION.equals(sessionId)) {
            session = sessions.get(sessionId);
            if (session == null || !session.contains(resourceIndex)) {
                releaseTransfer(transferId, sessionId, resourceIndex);
                return;
            }
        } else if (resourceIndex != NO_RESOURCE) {
            releaseTransfer(transferId, sessionId, resourceIndex);
            return;
        }
        if (fragment.getDecodedSize() > ZstdPayloadCompression.MAX_DECOMPRESSED_PAYLOAD) {
            protocolAnomaly(ProtocolAnomaly.INVALID_FRAGMENT,
                    "Ignoring oversized model transfer fragment: session={}, resource={}, decodedBytes={}",
                    sessionId, resourceIndex, fragment.getDecodedSize());
            releaseTransfer(transferId, sessionId, resourceIndex);
            failTransfer(sessionId, resourceIndex,
                    new IOException("Invalid model transfer size"));
            return;
        }
        final InboundTransfer transfer;
        try {
            transfer = transfers.compute(transferId, (id, current) -> {
                if (releasedTransfers.contains(id) || processingTransfers.containsKey(id)) {
                    return null;
                }
                if (current == null) {
                    var reservation = transferBudget.reserve(
                            fragment.getTransferSize(), fragment.getDecodedSize());
                    try {
                        return new InboundTransfer(transferId, sessionId, resourceIndex,
                                new AssetFragmentAssembler(payload, MAX_COMPRESSED_TRANSFER,
                                        ZstdPayloadCompression.MAX_DECOMPRESSED_PAYLOAD), reservation);
                    } catch (RuntimeException error) {
                        reservation.close();
                        throw error;
                    }
                }
                current.assembly.add(payload);
                return current;
            });
        } catch (RuntimeException error) {
            releaseTransfer(transferId, sessionId, resourceIndex);
            var removed = transfers.remove(transferId);
            if (removed != null) {
                removed.close();
            }
            protocolAnomaly(ProtocolAnomaly.INVALID_FRAGMENT,
                    "Ignoring invalid model transfer fragment: session={}, resource={}, reason={}",
                    sessionId, resourceIndex, error.getMessage());
            failTransfer(sessionId, resourceIndex, error);
            return;
        }
        if (transfer == null) {
            return;
        }
        var assembly = transfer.assembly;
        if (!assembly.isComplete()
                || processingTransfers.putIfAbsent(transferId, transfer) != null) {
            return;
        }
        if (!transfers.remove(transferId, transfer)) {
            processingTransfers.remove(transferId, transfer);
            return;
        }
        var pendingSession = session;
        if (pendingSession != null && pendingSession.resultDone(assembly.resourceIndex())) {
            pendingSession.remoteTerminal(assembly.resourceIndex());
            transfer.close();
            processingTransfers.remove(transferId, transfer);
            return;
        }
        try {
            CompletableFuture.runAsync(() -> completeTransfer(transferId, transfer), workers);
        } catch (RuntimeException error) {
            transfer.close();
            processingTransfers.remove(transferId, transfer);
            failTransfer(sessionId, resourceIndex, error);
        }
    }

    public void requestFailed(AssetTransferV0.ModelAssetBatchFailure failure) {
        final UUID requestId;
        try {
            requestId = AssetTransferMessages.requestId(failure);
        } catch (RuntimeException error) {
            protocolAnomaly(ProtocolAnomaly.INVALID_FAILURE,
                    "Ignoring model asset failure with invalid identity: {}", error.getMessage());
            return;
        }
        var session = sessions.get(requestId);
        if (session == null) {
            return;
        }
        final AssetTransferV0.AssetFailureReason reason;
        final int resourceIndex;
        if (failure.hasSession()) {
            reason = failure.getSession().getReason();
            resourceIndex = -1;
        } else if (failure.hasResource()) {
            reason = failure.getResource().getReason();
            resourceIndex = failure.getResource().getResourceIndex();
        } else {
            protocolAnomaly(ProtocolAnomaly.INVALID_FAILURE,
                    "Ignoring model asset failure without a session or resource result: request={}",
                    requestId);
            return;
        }
        if (failure.hasResource()
                && (resourceIndex < 0 || resourceIndex >= session.resources.length)) {
            protocolAnomaly(ProtocolAnomaly.INVALID_FAILURE,
                    "Ignoring model asset failure with invalid resource index: request={}, resource={}",
                    requestId, resourceIndex);
            return;
        }
        YesSteveModel.LOGGER.debug(
                "Model asset request rejected by server: request={} resource={} reason={}",
                requestId, resourceIndex, reason);
        var error = new IOException("Server rejected model asset request: " + reason);
        if (failure.hasSession()) {
            session.fail(error);
        } else {
            session.fail(resourceIndex, error);
        }
        if (reason == AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_STALE_CATALOG) {
            networkSender.accept(AssetTransferMessages.resync(session.cursor));
        }
    }

    public void cleanUp() {
        var cutoff = System.nanoTime() - TRANSFER_TIMEOUT_NANOS;
        transfers.forEach((id, transfer) -> {
            if (transfer.createdAt() < cutoff && closeTransfer(transfers, id, transfer)) {
                failTransfer(transfer.assembly.requestId(), transfer.assembly.resourceIndex(),
                        new IOException("Model transfer timed out"));
            }
        });
        processingTransfers.forEach((id, transfer) -> {
            if (transfer.createdAt() < cutoff
                    && closeTransfer(processingTransfers, id, transfer)) {
                failTransfer(transfer.assembly.requestId(), transfer.assembly.resourceIndex(),
                        new IOException("Model transfer processing timed out"));
            }
        });
    }

    public void disconnect() {
        connected = false;
        sessions.values().forEach(PendingSession::disconnect);
        sessions.clear();
        transfers.values().forEach(InboundTransfer::close);
        transfers.clear();
        processingTransfers.values().forEach(InboundTransfer::close);
        processingTransfers.clear();
        releasedTransfers.clear();
        protocolAnomalies.clear();
    }

    private void completeTransfer(UUID transferId, InboundTransfer transfer) {
        var assembly = transfer.assembly;
        var kind = TransferKind.fromProto(assembly.kind());
        var sessionId = assembly.requestId();
        var resourceIndex = assembly.resourceIndex();
        try (var content = assembly.acquireContent()) {
            if (kind == TransferKind.CATALOG_FULL || kind == TransferKind.CATALOG_DELTA) {
                if (assembly.encoding() == AssetTransferV0.TransferPayloadEncoding
                        .TRANSFER_PAYLOAD_ENCODING_ZSTD) {
                    catalogConsumer.accept(kind,
                            ZstdPayloadCompression.decompress(content, assembly.decodedSize()));
                } else if (assembly.encoding() == AssetTransferV0.TransferPayloadEncoding
                        .TRANSFER_PAYLOAD_ENCODING_PROTOBUF
                        && content.size() == assembly.decodedSize()) {
                    catalogConsumer.accept(kind, content.acquireArray());
                } else {
                    throw new IOException("Invalid catalog transfer encoding");
                }
                return;
            }
            var session = sessions.get(sessionId);
            if (session == null) {
                return;
            }
            var pending = session.resource(resourceIndex);
            if (pending == null) {
                return;
            }
            if (assembly.encoding() != AssetTransferV0.TransferPayloadEncoding
                    .TRANSFER_PAYLOAD_ENCODING_RAW_ATTACHMENTS || assembly.manifest() == null) {
                throw new IOException("Invalid model asset transfer encoding");
            }
            var assets = new ReceivedModelAssets(assembly.manifest(), content.acquire());
            try {
                validateBundle(pending.entry, pending.subject, pending.selector, assets);
                session.complete(resourceIndex, assets);
            } catch (Throwable error) {
                assets.close();
                throw error;
            }
        } catch (Throwable error) {
            if (!NO_SESSION.equals(sessionId)) {
                protocolAnomaly(ProtocolAnomaly.INVALID_RESPONSE,
                        "Rejected invalid model asset response: session={}, resource={}, reason={}",
                        sessionId, resourceIndex, unwrap(error).getMessage());
            }
            failTransfer(sessionId, resourceIndex, error);
        } finally {
            transfer.close();
            processingTransfers.remove(transferId, transfer);
        }
    }

    private void failTransfer(UUID sessionId, int resourceIndex, Throwable error) {
        if (!NO_SESSION.equals(sessionId)) {
            var session = sessions.get(sessionId);
            if (session != null) {
                var resource = session.resource(resourceIndex);
                if (resource != null && !(unwrap(error) instanceof CancellationException)) {
                    YesSteveModel.LOGGER.debug(
                            "Failed to receive model asset subject={} selector={} session={} resource={}",
                            resource.subject, resource.selector, sessionId, resourceIndex, unwrap(error));
                }
                session.fail(resourceIndex, error);
            }
            return;
        }
        YesSteveModel.LOGGER.error("Failed to receive model catalog; requesting a full resync",
                unwrap(error));
        networkSender.accept(AssetTransferMessages.resync(null));
    }

    private void protocolAnomaly(ProtocolAnomaly anomaly, String message, Object... arguments) {
        if (protocolAnomalies.first(anomaly)) {
            YesSteveModel.LOGGER.warn(message, arguments);
        } else {
            YesSteveModel.LOGGER.debug(message, arguments);
        }
    }

    private static Throwable unwrap(Throwable error) {
        var current = error;
        while ((current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void removeTransfers(UUID sessionId) {
        transfers.forEach((transferId, transfer) -> {
            if (transfer.requestId().equals(sessionId)) {
                closeTransfer(transfers, transferId, transfer);
            }
        });
        processingTransfers.forEach((transferId, transfer) -> {
            if (transfer.requestId().equals(sessionId)) {
                closeTransfer(processingTransfers, transferId, transfer);
            }
        });
    }

    private static boolean closeTransfer(ConcurrentHashMap<UUID, InboundTransfer> owner,
                                         UUID transferId, InboundTransfer expected) {
        var removed = new AtomicBoolean();
        owner.computeIfPresent(transferId, (ignored, current) -> {
            if (current != expected) {
                return current;
            }
            current.close();
            removed.set(true);
            return null;
        });
        return removed.get();
    }

    private void releaseTransfer(UUID transferId, UUID requestId, int resourceIndex) {
        if (!releasedTransfers.add(transferId) || !connected) {
            return;
        }
        try {
            networkSender.accept(AssetTransferMessages.release(transferId, requestId, resourceIndex));
        } catch (Throwable error) {
            YesSteveModel.LOGGER.debug(
                    "Failed to release model transfer credit: transfer={} request={} resource={}",
                    transferId, requestId, resourceIndex, error);
        }
    }

    private static void validateBundle(ClientCatalogEntry entry, ModelAssetSubject expectedSubject,
                                       ModelAssetSelector selector,
                                       ReceivedModelAssets assets) throws IOException {
        var bundle = assets.manifest();
        var receivedSelector = ModelAssetProtoMapper.fromProto(bundle.getSelector());
        var receivedSubject = ModelAssetProtoMapper.fromProto(bundle.getSubject());
        if (!selector.equals(receivedSelector) || !expectedSubject.equals(receivedSubject)) {
            throw new IOException("Model asset response does not match its request");
        }
        long expectedOffset = 0;
        for (var chunk : bundle.getChunks()) {
            if (chunk.getRawOffset() != expectedOffset || chunk.getRawSize() < 0) {
                throw new IOException("Model asset response contains an invalid attachment layout");
            }
            expectedOffset += chunk.getRawSize();
        }
        if (expectedOffset != assets.attachmentSize()) {
            throw new IOException("Model asset response does not cover its raw attachment");
        }
        if (selector instanceof ModelAssetSelector.PackCover cover) {
            if (!(receivedSubject instanceof ModelAssetSubject.Pack)) {
                throw new IOException("Model pack response contains the wrong subject");
            }
            if (bundle.getChunks().length() != 1
                    || !"pack-cover".equals(bundle.getChunks().get(0).getName())
                    || !ProtoBytes.equals(cover.expectedContentHash(),
                    bundle.getChunks().get(0).getContentHash())
                    || !hash(assets, bundle.getChunks().get(0)).equals(cover.expectedContentHash())) {
                throw new IOException("Invalid model pack cover response");
            }
            return;
        }
        if (entry == null || entry.server() == null
                || !expectedSubject.equals(new ModelAssetSubject.Model(entry.modelHash(),
                entry.server().descriptor().descriptorHash()))) {
            throw new IOException("Model asset response does not match its request");
        }
        if (selector instanceof ModelAssetSelector.RenderTarget target
                && target.components().contains(ModelAssetSelector.RenderTargetComponent.DEFINITION)
                && (!ProtoBytes.equals(entry.server().descriptor().containerPreamble(), bundle.getContainerPreamble())
                || !ProtoBytes.equals(entry.server().descriptor().schemaManifest(), bundle.getSchemaManifest()))) {
            throw new IOException("Model asset response metadata does not match the catalog");
        }
        var expectedChunks = ModelAssetPlan.chunks(entry.server().descriptor().view().getManifest(), selector);
        var received = new HashSet<String>();
        for (var chunk : bundle.getChunks()) {
            if (!expectedChunks.contains(chunk.getName()) || !received.add(chunk.getName())) {
                throw new IOException("Unexpected or duplicate model chunk: " + chunk.getName());
            }
        }
        if (!received.equals(expectedChunks)) {
            throw new IOException("Model asset response is missing required chunks");
        }
    }

    private static ModelHash hash(ReceivedModelAssets assets,
                                  ModelAssetsProto.ModelChunk chunk) {
        try (var data = assets.chunk(chunk)) {
            return ModelHashing.blake3(data);
        }
    }

    public final class AssetBatchSession {
        private final UUID sessionId = UUID.randomUUID();
        private final CatalogCursor cursor;
        private final ArrayList<PreparedResource> resources = new ArrayList<>();
        private boolean submitted;

        private AssetBatchSession(CatalogCursor cursor) {
            this.cursor = cursor;
        }

        public synchronized CompletableFuture<ReceivedModelAssets> request(
                TaskContext context, ClientCatalogEntry entry, ModelAssetSelector selector,
                CompletionStage<Boolean> required) {
            if (submitted) {
                throw new IllegalStateException("Asset batch was already submitted");
            }
            var descriptor = Objects.requireNonNull(entry.server(), "server offer").descriptor();
            var subject = new ModelAssetSubject.Model(entry.modelHash(), descriptor.descriptorHash());
            var resource = new PreparedResource(entry, subject, selector, required);
            resources.add(resource);
            resource.setGuard(context.guard(resource::cancel));
            return resource.result;
        }

        public synchronized CompletableFuture<ReceivedModelAssets> requestPack(
                TaskContext context, ModelAssetSubject.Pack subject, ModelAssetSelector.PackCover selector,
                CompletionStage<Boolean> required) {
            if (submitted) {
                throw new IllegalStateException("Asset batch was already submitted");
            }
            var resource = new PreparedResource(null, subject, selector, required);
            resources.add(resource);
            resource.setGuard(context.guard(resource::cancel));
            return resource.result;
        }

        public void submit() {
            final List<PreparedResource> submittedResources;
            synchronized (this) {
                if (submitted) {
                    throw new IllegalStateException("Asset batch was already submitted");
                }
                submitted = true;
                submittedResources = List.copyOf(resources);
            }
            if (submittedResources.isEmpty()) {
                return;
            }
            CompletableFuture.allOf(submittedResources.stream()
                    .map(resource -> resource.requirement).toArray(CompletableFuture[]::new))
                    .whenComplete((ignored, error) -> prepare(submittedResources));
        }

        private void prepare(List<PreparedResource> submittedResources) {
            var pending = new ArrayList<PreparedResource>();
            for (var resource : submittedResources) {
                var requirement = resource.requirement.join();
                if (resource.result.isDone()) {
                    continue;
                }
                if (requirement.error != null) {
                    resource.completeExceptionally(requirement.error);
                } else if (!requirement.required) {
                    resource.complete(null);
                } else if (!connected) {
                    resource.completeExceptionally(new IOException("Model server is not connected"));
                } else if (!(resource.selector instanceof ModelAssetSelector.PackCover)
                        && (resource.entry == null || resource.entry.server() == null)) {
                    resource.completeExceptionally(
                            new IOException("Model is not available from the current server"));
                } else {
                    pending.add(resource);
                }
            }
            if (pending.isEmpty()) {
                return;
            }
            if (pending.size() > ProtocolLimits.MAX_ASSET_RESOURCES) {
                var error = new IOException("Model asset batch contains too many remote resources");
                pending.forEach(resource -> resource.completeExceptionally(error));
                return;
            }
            var session = new PendingSession(sessionId, cursor, pending);
            sessions.put(sessionId, session);
            session.send();
        }
    }

    private final class PendingSession {
        private final UUID sessionId;
        private final CatalogCursor cursor;
        private final PreparedResource[] resources;
        private final boolean[] remoteTerminal;
        private boolean sent;
        private boolean cancelSent;
        private boolean serverTerminated;
        private boolean finished;

        private PendingSession(UUID sessionId, CatalogCursor cursor,
                               List<PreparedResource> resources) {
            this.sessionId = sessionId;
            this.cursor = cursor;
            this.resources = resources.toArray(PreparedResource[]::new);
            this.remoteTerminal = new boolean[this.resources.length];
            for (var resource : this.resources) {
                resource.activate(this);
            }
        }

        private void send() {
            final AssetTransferV0.ModelAssetBatchRequest request;
            synchronized (this) {
                if (allResultsDone()) {
                    finish(false);
                    return;
                }
                if (cursor == null) {
                    fail(new IOException("The model source catalog is not available"));
                    return;
                }
                request = AssetTransferV0.ModelAssetBatchRequest.newInstance()
                        .setCursor(AssetTransferMessages.cursor(cursor));
                ProtocolUuid.set(request.getMutableRequestId(), sessionId);
                for (var resource : resources) {
                    var outbound = AssetTransferV0.AssetResourceRequest.newInstance()
                            .setSubject(ModelAssetProtoMapper.toProto(resource.subject))
                            .setSelector(ModelAssetProtoMapper.toProto(resource.selector));
                    request.addResources(outbound);
                }
                sent = true;
            }
            try {
                networkSender.accept(request);
            } catch (Throwable error) {
                fail(error);
            }
        }

        private synchronized boolean contains(int resourceIndex) {
            return resourceIndex >= 0 && resourceIndex < resources.length;
        }

        private synchronized PreparedResource resource(int resourceIndex) {
            return contains(resourceIndex) ? resources[resourceIndex] : null;
        }

        private synchronized boolean resultDone(int resourceIndex) {
            return contains(resourceIndex) && resources[resourceIndex].result.isDone();
        }

        private void complete(int resourceIndex, ReceivedModelAssets bundle) {
            synchronized (this) {
                if (!contains(resourceIndex) || finished) {
                    return;
                }
                remoteTerminal[resourceIndex] = true;
                resources[resourceIndex].complete(bundle);
                finishIfDone();
            }
        }

        private void fail(int resourceIndex, Throwable error) {
            synchronized (this) {
                if (!contains(resourceIndex) || finished) {
                    return;
                }
                remoteTerminal[resourceIndex] = true;
                resources[resourceIndex].completeExceptionally(error);
                finishIfDone();
            }
        }

        private void fail(Throwable error) {
            synchronized (this) {
                if (finished) {
                    return;
                }
                serverTerminated = true;
                Arrays.fill(remoteTerminal, true);
                for (var resource : resources) {
                    resource.completeExceptionally(error);
                }
                finish(false);
            }
        }

        private void remoteTerminal(int resourceIndex) {
            synchronized (this) {
                if (!contains(resourceIndex) || finished) {
                    return;
                }
                remoteTerminal[resourceIndex] = true;
                finishIfDone();
            }
        }

        private void resourceCancelled() {
            synchronized (this) {
                finishIfDone();
            }
        }

        private void disconnect() {
            synchronized (this) {
                if (finished) {
                    return;
                }
                serverTerminated = true;
                for (var resource : resources) {
                    resource.completeExceptionally(
                            new IOException("Disconnected while loading model data"));
                }
                finish(false);
            }
        }

        private void finishIfDone() {
            if (!allResultsDone()) {
                return;
            }
            var incompleteRemote = false;
            for (var terminal : remoteTerminal) {
                incompleteRemote |= !terminal;
            }
            finish(sent && incompleteRemote && !serverTerminated);
        }

        private boolean allResultsDone() {
            for (var resource : resources) {
                if (!resource.result.isDone()) {
                    return false;
                }
            }
            return true;
        }

        private void finish(boolean cancelServer) {
            if (finished) {
                return;
            }
            finished = true;
            sessions.remove(sessionId, this);
            removeTransfers(sessionId);
            if (cancelServer && connected && !cancelSent) {
                cancelSent = true;
                networkSender.accept(AssetTransferMessages.cancel(sessionId));
            }
        }
    }

    private static final class PreparedResource {
        private final ClientCatalogEntry entry;
        private final ModelAssetSubject subject;
        private final ModelAssetSelector selector;
        private final CompletableFuture<Requirement> requirement;
        private final CompletableFuture<ReceivedModelAssets> result = new CompletableFuture<>();
        private volatile Closeable guard;
        private PendingSession session;

        private PreparedResource(ClientCatalogEntry entry, ModelAssetSubject subject,
                                 ModelAssetSelector selector,
                                 CompletionStage<Boolean> required) {
            this.entry = entry;
            this.subject = subject;
            this.selector = selector;
            this.requirement = required.handle((value, error) ->
                    new Requirement(Boolean.TRUE.equals(value), error)).toCompletableFuture();
        }

        private synchronized void activate(PendingSession session) {
            this.session = session;
        }

        private void setGuard(Closeable next) {
            var close = false;
            synchronized (this) {
                if (result.isDone()) {
                    close = true;
                } else {
                    guard = next;
                }
            }
            if (close && next != null) {
                next.close();
            }
        }

        private void cancel() {
            PendingSession current;
            synchronized (this) {
                if (result.isDone()) {
                    return;
                }
                result.completeExceptionally(
                        new CancellationException("Model asset resource was cancelled"));
                current = session;
            }
            closeGuard();
            if (current != null) {
                current.resourceCancelled();
            }
        }

        private void complete(ReceivedModelAssets bundle) {
            if (result.complete(bundle)) {
                closeGuard();
            } else {
                bundle.close();
            }
        }

        private void completeExceptionally(Throwable error) {
            if (result.completeExceptionally(error)) {
                closeGuard();
            }
        }

        private void closeGuard() {
            Closeable current;
            synchronized (this) {
                current = guard;
                guard = null;
            }
            if (current != null) {
                current.close();
            }
        }
    }

    private record Requirement(boolean required, Throwable error) {
    }

    private enum ProtocolAnomaly {
        INVALID_FRAGMENT_IDENTITY,
        INVALID_FRAGMENT,
        INVALID_FAILURE,
        INVALID_RESPONSE
    }

    private final class InboundTransfer implements AutoCloseable {
        private final UUID transferId;
        private final UUID requestId;
        private final int resourceIndex;
        private final AssetFragmentAssembler assembly;
        private final AssetTransferBudget.Reservation reservation;
        private final AtomicBoolean closed = new AtomicBoolean();

        private InboundTransfer(UUID transferId, UUID requestId, int resourceIndex,
                                AssetFragmentAssembler assembly,
                                AssetTransferBudget.Reservation reservation) {
            this.transferId = transferId;
            this.requestId = requestId;
            this.resourceIndex = resourceIndex;
            this.assembly = assembly;
            this.reservation = reservation;
        }

        private long createdAt() {
            return assembly.createdAt();
        }

        private UUID requestId() {
            return assembly.requestId();
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                try {
                    assembly.close();
                } finally {
                    reservation.close();
                    releaseTransfer(transferId, requestId, resourceIndex);
                }
            }
        }
    }

}
