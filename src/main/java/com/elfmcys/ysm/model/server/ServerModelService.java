package com.elfmcys.ysm.model.server;

import com.elfmcys.ysm.config.ServerConfig;
import com.elfmcys.ysm.model.ModelRuntime;
import com.elfmcys.ysm.model.catalog.CatalogBackingKey;
import com.elfmcys.ysm.model.catalog.ReloadableModelCatalog;
import com.elfmcys.ysm.model.catalog.ServerCatalogSnapshot;
import com.elfmcys.ysm.model.domain.ModelScanReport;
import com.elfmcys.ysm.network.NetworkHandler;
import com.elfmcys.ysm.network.message.model.AssetTransferMessages;
import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/** Lifecycle and external API facade for server-side model management. */
public final class ServerModelService implements AutoCloseable {
    private static volatile ServerModelService INSTANCE;

    private final ScheduledThreadPoolExecutor workers;
    private final ReloadableModelCatalog reloadableCatalog;
    private final ServerCatalogManager catalogs;
    private final ServerAssetRequestHandler assets;

    private ServerModelService(MinecraftServer server) {
        var configured = ServerConfig.THREAD_COUNT.get();
        var threadCount = configured == 0
                ? Math.max(2, Runtime.getRuntime().availableProcessors() / 2)
                : configured;
        workers = new ScheduledThreadPoolExecutor(threadCount, runnable -> {
            var thread = new Thread(runnable, "YSM Model Worker");
            thread.setDaemon(true);
            return thread;
        });
        workers.setRemoveOnCancelPolicy(true);

        var system = ModelRuntime.system();
        reloadableCatalog = system.reloadableCatalog();
        assets = new ServerAssetRequestHandler(workers, new ServerAssetRequestHandler.BackingRecovery() {
            @Override
            public CompletableFuture<ServerCatalogSnapshot> recover(CatalogBackingKey backing) {
                return recoverBacking(backing);
            }

            @Override
            public CompletableFuture<ServerCatalogSnapshot> audit() {
                return recoverCatalog();
            }
        });
        catalogs = new ServerCatalogManager(server, workers, reloadableCatalog,
                system.builtins().snapshot(),
                outbound -> {
                    for (var player : server.getPlayerList().getPlayers()) {
                        if (NetworkHandler.isPlayerChannelPresent(player)) {
                            assets.sendCatalog(player, outbound);
                        }
                    }
                });
    }

    public static synchronized ServerModelService start(MinecraftServer server) {
        if (INSTANCE != null) {
            throw new IllegalStateException("Server model service is already running");
        }
        INSTANCE = new ServerModelService(server);
        return INSTANCE;
    }

    public static ServerModelService instance() {
        var service = INSTANCE;
        if (service == null) {
            throw new IllegalStateException("Server model service is not running");
        }
        return service;
    }

    public static Optional<ServerModelService> current() {
        return Optional.ofNullable(INSTANCE);
    }

    public Optional<ServerCatalogSnapshot> snapshot() {
        return catalogs.snapshot();
    }

    public ModelScanReport lastReport() {
        return catalogs.lastReport();
    }

    public CompletableFuture<ReloadOutcome> reload(boolean notifyPlayers) {
        return reloadableCatalog.reload().thenCompose(result -> {
            if (!result.committed()) {
                return CompletableFuture.completedFuture(new ReloadOutcome(false,
                        result.modelCount(), result.errorCount(), result.failureMessage()));
            }
            return catalogs.awaitApplied(result.reloadGeneration()).thenApply(ignored ->
                    new ReloadOutcome(true, result.modelCount(), result.errorCount(), ""));
        });
    }

    public void sendCatalog(ServerPlayer player) {
        catalogs.fullCatalog().whenComplete((payload, error) -> {
            if (error == null && player.connection.isAcceptingMessages()) {
                try (payload) {
                    assets.sendCatalog(player, payload);
                }
            }
        });
    }

    public void handleRequest(ServerPlayer player, AssetTransferV0.ModelAssetBatchRequest request) {
        final UUID requestId;
        try {
            requestId = AssetTransferMessages.requestId(request);
        } catch (RuntimeException error) {
            assets.protocolAnomaly(player, ServerAssetRequestHandler.ProtocolAnomaly.INVALID_REQUEST_ID,
                    "Rejected model asset request with invalid identity from player={}: reason={}",
                    player.getUUID(), error.getMessage());
            return;
        }
        var snapshot = catalogs.snapshot().orElse(null);
        if (snapshot == null) {
            assets.failSession(player, requestId,
                    AssetTransferV0.AssetFailureReason.ASSET_FAILURE_REASON_STALE_CATALOG);
            return;
        }
        assets.handleBatch(player, request, snapshot);
    }

    public void cancelSession(UUID playerId, UUID sessionId) {
        assets.cancelSession(playerId, sessionId);
    }

    public void releaseTransfer(UUID playerId, AssetTransferV0.AssetTransferRelease release) {
        assets.releaseTransfer(playerId, release);
    }

    public void playerDisconnected(UUID playerId) {
        assets.playerDisconnected(playerId);
    }

    private CompletableFuture<ServerCatalogSnapshot> recoverBacking(CatalogBackingKey backing) {
        var recovery = reloadableCatalog.recoverUntilSettled(backing);
        return cancellablePublishedSnapshot(recovery, "recovery");
    }

    private CompletableFuture<ServerCatalogSnapshot> recoverCatalog() {
        var recovery = reloadableCatalog.auditUntilSettled();
        return cancellablePublishedSnapshot(recovery, "audit");
    }

    private CompletableFuture<ServerCatalogSnapshot> cancellablePublishedSnapshot(
            CompletableFuture<com.elfmcys.ysm.model.catalog.ReloadResult> recovery,
            String operation) {
        var published = recovery.thenCompose(result ->
                        catalogs.awaitApplied(result.reloadGeneration()))
                .thenApply(ignored -> catalogs.snapshot().orElseThrow(() ->
                        new IllegalStateException(
                                "Server catalog is unavailable after " + operation)));
        var result = new CompletableFuture<ServerCatalogSnapshot>();
        published.whenComplete((snapshot, error) -> {
            if (error == null) {
                result.complete(snapshot);
            } else {
                result.completeExceptionally(error);
            }
        });
        result.whenComplete((ignored, error) -> {
            if (result.isCancelled()) {
                recovery.cancel(false);
                published.cancel(false);
            }
        });
        return result;
    }

    @Override
    public synchronized void close() {
        if (INSTANCE == this) {
            INSTANCE = null;
        }
        assets.close();
        catalogs.close();
        workers.shutdownNow();
    }

    public record ReloadOutcome(boolean success, int modelCount, int errorCount, String message) {
    }
}
