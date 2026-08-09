package com.elfmcys.ysm.model.server;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.capability.AuthModelsCapabilityProvider;
import com.elfmcys.ysm.model.cache.IdleValueCache;
import com.elfmcys.ysm.model.catalog.ModelCatalogSources;
import com.elfmcys.ysm.model.catalog.CatalogBackingKey;
import com.elfmcys.ysm.model.catalog.ServerCatalogSnapshot;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import com.elfmcys.ysm.model.source.AccessPolicy;
import com.elfmcys.ysm.model.storage.ModelHashing;
import com.elfmcys.ysm.model.storage.ModelBackingException;
import com.elfmcys.ysm.model.storage.ModelFileHandle;
import com.elfmcys.ysm.network.NetworkHandler;
import com.elfmcys.ysm.network.message.model.PreparedTransfer;
import com.elfmcys.ysm.network.message.model.ModelAssetAssembler;
import com.elfmcys.ysm.model.source.ModelAssetSelector;
import com.elfmcys.ysm.model.source.ModelAssetSubject;
import com.elfmcys.ysm.network.message.model.ModelAssetProtoMapper;
import com.elfmcys.ysm.network.message.model.ModelTransferSender;
import com.elfmcys.ysm.network.message.model.TransferKind;
import com.elfmcys.ysm.network.message.model.AssetTransferMessages;
import com.elfmcys.ysm.network.protocol.ProtocolLimits;
import com.elfmcys.ysm.network.protocol.FirstOccurrenceTracker;
import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;
import com.elfmcys.ysm.task.TaskScope;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Authorizes and independently streams every resource in a cancellable batch session. */
final class ServerAssetRequestHandler implements AutoCloseable {
    private static final UUID NO_SESSION = new UUID(0, 0);
    private static final int NO_RESOURCE = -1;

    private final ScheduledThreadPoolExecutor workers;
    private final BackingRecovery recovery;
    private final TaskScope taskScope;
    private final ModelAssetAssembler assembler = new ModelAssetAssembler();
    private final ModelTransferSender sender;
    private final IdleValueCache<PreviewKey, PreparedTransfer> previews =
            new IdleValueCache<>(Duration.ofSeconds(30), PreparedTransfer::close);
    private final IdleValueCache<PackCoverKey, PreparedTransfer> packCovers =
            new IdleValueCache<>(Duration.ofSeconds(30), PreparedTransfer::close);
    private final ConcurrentHashMap<SessionKey, RequestSession> sessions = new ConcurrentHashMap<>();
    private final FirstOccurrenceTracker<ProtocolAnomalyKey> protocolAnomalies =
            new FirstOccurrenceTracker<>();

    ServerAssetRequestHandler(ScheduledThreadPoolExecutor workers, BackingRecovery recovery) {
        this.workers = workers;
        this.recovery = recovery;
        this.taskScope = TaskScope.create(workers);
        this.sender = new ModelTransferSender(workers);
        workers.scheduleAtFixedRate(previews::cleanUp, 5, 5, TimeUnit.SECONDS);
        workers.scheduleAtFixedRate(packCovers::cleanUp, 5, 5, TimeUnit.SECONDS);
    }

    void sendCatalog(ServerPlayer player, PreparedTransfer payload) {
        var transfer = sender.send(player, NO_SESSION, NO_RESOURCE, TransferKind.CATALOG_FULL, payload);
        YesSteveModel.LOGGER.debug(
                "Queued full model catalog for player={} compressedBytes={} decodedBytes={}",
                player.getUUID(), payload.contentSize(), payload.decodedSize());
        transfer.completion().whenComplete((ignored, error) -> {
            if (error != null) {
                YesSteveModel.LOGGER.debug("Failed to send full model catalog to player={}",
                        player.getUUID(), unwrap(error));
            }
        });
    }

    void sendCatalog(ServerPlayer player, ServerCatalogManager.OutboundCatalog outbound) {
        var transfer = sender.send(player, NO_SESSION, NO_RESOURCE, outbound.kind(), outbound.payload());
        YesSteveModel.LOGGER.debug(
                "Queued model catalog update for player={} kind={} compressedBytes={} decodedBytes={}",
                player.getUUID(), outbound.kind(), outbound.payload().contentSize(),
                outbound.payload().decodedSize());
        transfer.completion().whenComplete((ignored, error) -> {
            if (error != null) {
                YesSteveModel.LOGGER.debug("Failed to send model catalog update to player={} kind={}",
                        player.getUUID(), outbound.kind(), unwrap(error));
            }
        });
    }

    void handleBatch(ServerPlayer player, AssetTransferV0.ModelAssetBatchRequest request,
                     ServerCatalogSnapshot snapshot) {
        final RequestSession session;
        try {
            session = register(player, request);
        } catch (RuntimeException error) {
            return;
        }
        if (!request.hasCursor() || !AssetTransferMessages.matches(
                request.getCursor(), snapshot.epochBytes(), snapshot.revision())) {
            failSession(session, AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_STALE_CATALOG);
            return;
        }
        for (var index = 0; index < request.getResources().length(); index++) {
            serve(session, index, request.getResources().get(index), snapshot);
        }
    }

    void cancelSession(UUID playerId, UUID sessionId) {
        var session = sessions.get(new SessionKey(playerId, sessionId));
        if (session == null) {
            sender.cancel(playerId, sessionId);
            return;
        }
        session.cancel();
    }

    void releaseTransfer(UUID playerId, AssetTransferV0.AssetTransferRelease release) {
        sender.release(playerId, release);
    }

    void playerDisconnected(UUID playerId) {
        sessions.forEach((key, session) -> {
            if (key.playerId.equals(playerId)) {
                session.cancel();
            }
        });
        sender.playerDisconnected(playerId);
        protocolAnomalies.removeIf(key -> key.playerId().equals(playerId));
    }

    void failSession(ServerPlayer player, UUID sessionId,
                     AssetTransferV0.AssetFailureReason reason) {
        NetworkHandler.sendToClientPlayer(
                AssetTransferMessages.sessionFailure(sessionId, reason), player);
    }

    private RequestSession register(ServerPlayer player, AssetTransferV0.ModelAssetBatchRequest request) {
        var requestId = AssetTransferMessages.requestId(request);
        var resourceCount = request.getResources().length();
        if (resourceCount == 0 || resourceCount > ProtocolLimits.MAX_ASSET_RESOURCES) {
            protocolAnomaly(player, ProtocolAnomaly.INVALID_RESOURCE_COUNT,
                    "Rejected invalid model asset resource count from player={}: count={}",
                    player.getUUID(), resourceCount);
            failSession(player, requestId,
                    AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_INVALID_REQUEST);
            throw new IllegalArgumentException("Invalid asset resource count");
        }
        var key = new SessionKey(player.getUUID(), requestId);
        var created = new RequestSession(key, player, resourceCount);
        var previous = sessions.put(key, created);
        if (previous != null) {
            protocolAnomaly(player, ProtocolAnomaly.DUPLICATE_SESSION,
                    "Replaced duplicate model asset session from player={}: session={}",
                    player.getUUID(), requestId);
            previous.cancel();
        }
        return created;
    }

    private void serve(RequestSession session, int resourceIndex,
                       AssetTransferV0.AssetResourceRequest wireResource,
                       ServerCatalogSnapshot snapshot) {
        final DecodedResource resource;
        try {
            resource = decode(wireResource);
        } catch (IOException | RuntimeException error) {
            protocolAnomaly(session.player, ProtocolAnomaly.INVALID_RESOURCE,
                    "Rejected invalid model asset resource from player={}: session={}, resource={}, reason={}",
                    session.player.getUUID(), session.key.sessionId, resourceIndex, error.getMessage());
            failResource(session, resourceIndex,
                    AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_INVALID_REQUEST);
            return;
        }
        var selector = resource.selector();
        if (selector instanceof ModelAssetSelector.PackCover cover) {
            if (!(resource.subject() instanceof ModelAssetSubject.Pack pack)) {
                protocolAnomaly(session.player, ProtocolAnomaly.SUBJECT_SELECTOR_MISMATCH,
                        "Rejected mismatched model asset subject from player={}: session={}, resource={}",
                        session.player.getUUID(), session.key.sessionId, resourceIndex);
                failResource(session, resourceIndex,
                        AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_INVALID_REQUEST);
                return;
            }
            servePackCover(session, resourceIndex, pack, cover, snapshot);
            return;
        }
        if (!(resource.subject() instanceof ModelAssetSubject.Model model)) {
            protocolAnomaly(session.player, ProtocolAnomaly.SUBJECT_SELECTOR_MISMATCH,
                    "Rejected mismatched model asset subject from player={}: session={}, resource={}",
                    session.player.getUUID(), session.key.sessionId, resourceIndex);
            failResource(session, resourceIndex,
                    AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_INVALID_REQUEST);
            return;
        }
        var handle = snapshot.models().get(model.modelHash());
        if (handle == null) {
            failResource(session, resourceIndex,
                    AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_NOT_FOUND);
            return;
        }
        if (com.elfmcys.ysm.model.catalog.ServerCatalogEncoder
                .isIntrinsicDefault(handle.location())) {
            failResource(session, resourceIndex,
                    AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_NOT_FOUND);
            return;
        }
        if (!handle.descriptor().descriptorHash().equals(model.descriptorHash())) {
            failResource(session, resourceIndex,
                    AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_STALE_CATALOG);
            return;
        }
        if (!ModelAssetProtoMapper.isPublicPresentation(selector)
                && !canDownload(session.player, model.modelHash(),
                handle.location().rootKind().accessPolicy())) {
            failResource(session, resourceIndex,
                    AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_FORBIDDEN);
            return;
        }

        prepareModelResource(session, resourceIndex, model, selector, handle);
    }

    private void prepareModelResource(RequestSession session, int resourceIndex,
                                      ModelAssetSubject.Model model,
                                      ModelAssetSelector selector,
                                      ModelFileHandle handle) {
        var cachedPayload = selector instanceof ModelAssetSelector.ModelPreview;
        CompletableFuture<PreparedTransfer> payload;
        if (cachedPayload) {
            var key = new PreviewKey(model.modelHash(), model.descriptorHash());
            payload = previews.get(key, ignored -> assembler.assemble(taskScope, handle, selector));
        } else {
            payload = assembler.assemble(session.scope, handle, selector);
        }
        var startedAt = System.nanoTime();
        payload.whenComplete((result, error) -> {
            if (error == null) {
                YesSteveModel.LOGGER.debug(
                        "Prepared model asset hash={} selector={} player={} session={} resource={} wireBytes={} decodedBytes={} elapsedMs={}",
                        model.modelHash(), selector, session.player.getUUID(), session.key.sessionId,
                        resourceIndex, result.contentSize(), result.decodedSize(),
                        Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
                try {
                    session.send(resourceIndex, result);
                } finally {
                    if (!cachedPayload) {
                        result.close();
                    }
                }
                return;
            }
            if (session.cancelled()) {
                return;
            }
            var cause = unwrap(error);
            if (cause instanceof ModelBackingException backing
                    && handle.location().rootKind() != CatalogRootKind.BUILTIN) {
                recoverModelResource(session, resourceIndex, model, selector, handle, backing);
                return;
            }
            if (cause instanceof IllegalArgumentException) {
                protocolAnomaly(session.player, ProtocolAnomaly.INVALID_SELECTOR,
                        "Rejected invalid model asset selector from player={}: session={}, resource={}, selector={}, reason={}",
                        session.player.getUUID(), session.key.sessionId, resourceIndex,
                        selector, cause.getMessage());
            }
            YesSteveModel.LOGGER.debug(
                    "Failed to serve model asset hash={} selector={} player={} session={} resource={}",
                    model.modelHash(), selector, session.player.getUUID(), session.key.sessionId,
                    resourceIndex, cause);
            failResource(session, resourceIndex, cause instanceof IllegalArgumentException
                    ? AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_INVALID_REQUEST
                    : AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_INTERNAL);
        });
    }

    private void recoverModelResource(RequestSession session, int resourceIndex,
                                      ModelAssetSubject.Model model,
                                      ModelAssetSelector selector,
                                      ModelFileHandle failedHandle,
                                      ModelBackingException failure) {
        if (!failedHandle.backingIdentity().equals(failure.backingIdentity())) {
            failResource(session, resourceIndex,
                    AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_INTERNAL);
            return;
        }
        var key = new CatalogBackingKey(failedHandle.location(), failedHandle.backingIdentity());
        YesSteveModel.LOGGER.debug(
                "Model asset backing failed; request remains pending during recovery hash={} player={} session={} resource={}",
                model.modelHash(), session.player.getUUID(), session.key.sessionId, resourceIndex,
                failure);
        var ticket = recovery.recover(key);
        if (!session.trackRecovery(resourceIndex, ticket)) {
            ticket.cancel(false);
            return;
        }
        ticket.whenComplete((snapshot, error) -> {
            session.clearRecovery(resourceIndex, ticket);
            if (session.cancelled()) {
                return;
            }
            if (error != null) {
                YesSteveModel.LOGGER.error(
                        "Model backing recovery stopped before the request settled hash={} player={} session={} resource={}",
                        model.modelHash(), session.player.getUUID(), session.key.sessionId,
                        resourceIndex, unwrap(error));
                failResource(session, resourceIndex,
                        AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_INTERNAL);
                return;
            }
            var latest = snapshot.models().get(model.modelHash());
            if (latest == null || !latest.descriptor().descriptorHash()
                    .equals(model.descriptorHash())) {
                failResource(session, resourceIndex,
                        AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_STALE_CATALOG);
                return;
            }
            if (!ModelAssetProtoMapper.isPublicPresentation(selector)
                    && !canDownload(session.player, model.modelHash(),
                    latest.location().rootKind().accessPolicy())) {
                failResource(session, resourceIndex,
                        AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_FORBIDDEN);
                return;
            }
            prepareModelResource(session, resourceIndex, model, selector, latest);
        });
    }

    private void servePackCover(RequestSession session, int resourceIndex,
                                ModelAssetSubject.Pack subject,
                                ModelAssetSelector.PackCover selector,
                                ServerCatalogSnapshot snapshot) {
        var pack = snapshot.packs().stream()
                .filter(value -> value.rootKind().namespace().equals(subject.namespace())
                        && value.hierarchy().equals(subject.hierarchy()))
                .findFirst().orElse(null);
        if (pack == null || !selector.expectedContentHash().matches(pack.coverHash())) {
            failResource(session, resourceIndex,
                    AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_NOT_FOUND);
            return;
        }

        preparePackCover(session, resourceIndex, subject, selector, pack);
    }

    private void preparePackCover(RequestSession session, int resourceIndex,
                                  ModelAssetSubject.Pack subject,
                                  ModelAssetSelector.PackCover selector,
                                  com.elfmcys.ysm.model.domain.ModelPackDescriptor pack) {

        var expectedHash = selector.expectedContentHash();
        var key = new PackCoverKey(pack.rootKind(), pack.hierarchy(), expectedHash);
        var startedAt = System.nanoTime();
        packCovers.get(key, ignored -> CompletableFuture.supplyAsync(() -> {
            try {
                var root = ModelCatalogSources.sources().stream()
                        .filter(value -> value.rootKind() == pack.rootKind())
                        .findFirst().orElseThrow(() -> new IOException("Pack root is unavailable"));
                var hierarchy = pack.hierarchy().endsWith("/")
                        ? pack.hierarchy().substring(0, pack.hierarchy().length() - 1)
                        : pack.hierarchy();
                var rootPath = root.path().toAbsolutePath().normalize();
                var file = rootPath.resolve(hierarchy).resolve("ysm-pack.png").normalize();
                if (!file.startsWith(rootPath) || !Files.isRegularFile(file)) {
                    throw new IOException("Pack cover is unavailable");
                }
                var bytes = Files.readAllBytes(file);
                if (bytes.length != pack.coverSize()
                        || !ModelHashing.blake3(bytes).equals(expectedHash)) {
                    throw new IOException("Pack cover changed after catalog scan");
                }
                return assembler.singleAttachment(subject, selector, "pack-cover",
                        pack.coverFormat(), bytes.length, pack.coverHash(),
                        com.elfmcys.ysm.buffer.ArrayBuffer.move(bytes));
            } catch (IOException error) {
                throw new UncheckedIOException(error);
            }
        }, workers)).whenComplete((payload, error) -> {
            if (error == null) {
                YesSteveModel.LOGGER.debug(
                        "Prepared model pack cover root={} hierarchy={} player={} session={} resource={} wireBytes={} decodedBytes={} elapsedMs={}",
                        pack.rootKind(), pack.hierarchy(), session.player.getUUID(), session.key.sessionId,
                        resourceIndex, payload.contentSize(), payload.decodedSize(),
                        Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
                session.send(resourceIndex, payload);
                return;
            }
            if (session.cancelled()) {
                return;
            }
            YesSteveModel.LOGGER.debug(
                    "Failed to serve model pack cover root={} hierarchy={} player={} session={} resource={}",
                    pack.rootKind(), pack.hierarchy(), session.player.getUUID(), session.key.sessionId,
                    resourceIndex, unwrap(error));
            recoverPackCover(session, resourceIndex, subject, selector, pack);
        });
    }

    private void recoverPackCover(RequestSession session, int resourceIndex,
                                  ModelAssetSubject.Pack subject,
                                  ModelAssetSelector.PackCover selector,
                                  com.elfmcys.ysm.model.domain.ModelPackDescriptor failedPack) {
        YesSteveModel.LOGGER.debug(
                "Model pack cover read failed; request remains pending during catalog audit root={} hierarchy={} player={} session={} resource={}",
                failedPack.rootKind(), failedPack.hierarchy(), session.player.getUUID(),
                session.key.sessionId, resourceIndex);
        var ticket = recovery.audit();
        if (!session.trackRecovery(resourceIndex, ticket)) {
            ticket.cancel(false);
            return;
        }
        ticket.whenComplete((snapshot, error) -> {
            session.clearRecovery(resourceIndex, ticket);
            if (session.cancelled()) {
                return;
            }
            if (error != null) {
                YesSteveModel.LOGGER.error(
                        "Model pack recovery stopped before the request settled root={} hierarchy={} player={} session={} resource={}",
                        failedPack.rootKind(), failedPack.hierarchy(), session.player.getUUID(),
                        session.key.sessionId, resourceIndex, unwrap(error));
                failResource(session, resourceIndex,
                        AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_INTERNAL);
                return;
            }
            var latest = snapshot.packs().stream()
                    .filter(value -> value.rootKind() == failedPack.rootKind()
                            && value.hierarchy().equals(failedPack.hierarchy()))
                    .findFirst().orElse(null);
            if (latest == null || !selector.expectedContentHash().matches(latest.coverHash())) {
                failResource(session, resourceIndex,
                        AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_STALE_CATALOG);
                return;
            }
            preparePackCover(session, resourceIndex, subject, selector, latest);
        });
    }

    private static DecodedResource decode(AssetTransferV0.AssetResourceRequest resource)
            throws IOException {
        if (!resource.hasSubject() || !resource.hasSelector()) {
            throw new IOException("Asset subject or selector is missing");
        }
        return new DecodedResource(ModelAssetProtoMapper.fromProto(resource.getSubject()),
                ModelAssetProtoMapper.fromProto(resource.getSelector()));
    }

    private static boolean canDownload(ServerPlayer player, Hash256 hash, AccessPolicy accessPolicy) {
        if (accessPolicy != AccessPolicy.SESSION_AUTHORIZED) {
            return true;
        }
        return player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP)
                .map(capability -> capability.containModel(hash)).orElse(false);
    }

    void protocolAnomaly(ServerPlayer player, ProtocolAnomaly anomaly,
                         String message, Object... arguments) {
        var key = new ProtocolAnomalyKey(player.getUUID(), anomaly);
        if (protocolAnomalies.first(key)) {
            YesSteveModel.LOGGER.warn(message, arguments);
        } else {
            YesSteveModel.LOGGER.debug(message, arguments);
        }
    }

    private void failResource(RequestSession session, int resourceIndex,
                              AssetTransferV0.AssetFailureReason reason) {
        if (!session.cancelled()) {
            NetworkHandler.sendToClientPlayer(
                    AssetTransferMessages.resourceFailure(
                            session.key.sessionId, resourceIndex, reason),
                    session.player);
        }
        session.terminal(resourceIndex);
    }

    private void failSession(RequestSession session, AssetTransferV0.AssetFailureReason reason) {
        if (!session.cancelled()) {
            failSession(session.player, session.key.sessionId, reason);
        }
        session.cancel();
    }

    private static Throwable unwrap(Throwable error) {
        var current = error;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    @Override
    public void close() {
        sessions.values().forEach(RequestSession::cancel);
        sessions.clear();
        previews.close();
        packCovers.close();
        taskScope.close();
        sender.close();
        protocolAnomalies.clear();
    }

    private record PreviewKey(Hash256 modelHash, Hash256 descriptorHash) {
    }

    private record PackCoverKey(CatalogRootKind rootKind, String hierarchy, Hash256 hash) {
    }

    private record DecodedResource(ModelAssetSubject subject, ModelAssetSelector selector) {
    }

    private record SessionKey(UUID playerId, UUID sessionId) {
    }

    enum ProtocolAnomaly {
        INVALID_REQUEST_ID,
        INVALID_RESOURCE_COUNT,
        DUPLICATE_SESSION,
        INVALID_RESOURCE,
        SUBJECT_SELECTOR_MISMATCH,
        INVALID_SELECTOR
    }

    interface BackingRecovery {
        CompletableFuture<ServerCatalogSnapshot> recover(CatalogBackingKey backing);

        CompletableFuture<ServerCatalogSnapshot> audit();
    }

    private record ProtocolAnomalyKey(UUID playerId, ProtocolAnomaly anomaly) {
    }

    private final class RequestSession {
        private final SessionKey key;
        private final ServerPlayer player;
        private final TaskScope scope = TaskScope.create(workers);
        private final ResourceState[] resources;
        private int remaining;
        private boolean cancelled;

        private RequestSession(SessionKey key, ServerPlayer player, int resourceCount) {
            this.key = key;
            this.player = player;
            this.resources = new ResourceState[resourceCount];
            for (var index = 0; index < resourceCount; index++) {
                resources[index] = new ResourceState();
            }
            remaining = resourceCount;
        }

        private synchronized boolean cancelled() {
            return cancelled;
        }

        private void send(int resourceIndex, PreparedTransfer payload) {
            synchronized (this) {
                if (cancelled || resources[resourceIndex].terminal) {
                    return;
                }
                resources[resourceIndex].transfer = sender.send(player, key.sessionId,
                        resourceIndex, TransferKind.MODEL_ASSET, payload);
                resources[resourceIndex].transfer.completion()
                        .whenComplete((ignored, error) -> {
                            if (error != null) {
                                YesSteveModel.LOGGER.debug(
                                        "Failed to stream model asset player={} session={} resource={}",
                                        player.getUUID(), key.sessionId, resourceIndex, unwrap(error));
                            }
                            terminal(resourceIndex);
                        });
            }
        }

        private void terminal(int resourceIndex) {
            var finish = false;
            synchronized (this) {
                if (cancelled || resources[resourceIndex].terminal) {
                    return;
                }
                resources[resourceIndex].terminal = true;
                if (resources[resourceIndex].recovery != null) {
                    resources[resourceIndex].recovery.cancel(false);
                    resources[resourceIndex].recovery = null;
                }
                remaining--;
                finish = remaining == 0;
            }
            if (finish) {
                sessions.remove(key, this);
                scope.close();
            }
        }

        private void cancel() {
            synchronized (this) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                for (var resource : resources) {
                    if (resource.recovery != null) {
                        resource.recovery.cancel(false);
                        resource.recovery = null;
                    }
                }
            }
            sessions.remove(key, this);
            scope.close();
            sender.cancel(key.playerId, key.sessionId);
        }

        private synchronized boolean trackRecovery(int resourceIndex,
                                                   CompletableFuture<?> recovery) {
            if (cancelled || resources[resourceIndex].terminal) {
                return false;
            }
            resources[resourceIndex].recovery = recovery;
            return true;
        }

        private synchronized void clearRecovery(int resourceIndex,
                                                CompletableFuture<?> recovery) {
            if (resources[resourceIndex].recovery == recovery) {
                resources[resourceIndex].recovery = null;
            }
        }
    }

    private static final class ResourceState {
        private ModelTransferSender.TransferHandle transfer;
        private CompletableFuture<?> recovery;
        private boolean terminal;
    }
}
