package com.elfmcys.ysm.client.model;

import com.elfmcys.ysm.client.model.catalog.ClientCatalogSnapshot;
import com.elfmcys.ysm.client.model.catalog.ModelContentVersion;
import com.elfmcys.ysm.client.model.internal.asset.ClientAssetRepository;
import com.elfmcys.ysm.client.model.internal.catalog.ClientCatalogManager;
import com.elfmcys.ysm.client.model.internal.render.BakedAnimationCache;
import com.elfmcys.ysm.client.model.internal.render.BakedModelCache;
import com.elfmcys.ysm.client.model.internal.render.ClientModelRenderTargetManager;
import com.elfmcys.ysm.client.model.internal.render.ModelRenderTargetLoader;
import com.elfmcys.ysm.client.model.internal.render.ModelRenderTargetRequestKey;
import com.elfmcys.ysm.client.model.internal.render.DefaultAnimationRuntime;
import com.elfmcys.ysm.client.model.internal.transfer.ClientTransferManager;
import com.elfmcys.ysm.client.texture.CustomTexture;
import com.elfmcys.ysm.model.ModelRuntime;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.domain.RenderTargetIds;
import com.elfmcys.ysm.model.storage.RemoteModelCache;
import com.elfmcys.ysm.natives.image.ImageSource;
import com.elfmcys.ysm.network.NetworkPayload;
import mixel.manifest.asset.RenderTargetOuterClass;
import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;
import com.elfmcys.ysm.task.TaskContext;
import com.elfmcys.ysm.task.TaskScope;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ClientModelService implements AutoCloseable {
    private static final long INITIALIZATION_TIMEOUT_SECONDS = 60;

    public enum State {
        NOT_STARTED,
        INITIALIZING,
        AVAILABLE,
        FAILED,
        CLOSED
    }

    public enum BuiltinReadiness {
        LOADING,
        READY,
        FAILED
    }

    private static volatile ClientModelService INSTANCE;
    private static volatile State STATE = State.NOT_STARTED;
    private static volatile Throwable STARTUP_FAILURE;

    private final ScheduledThreadPoolExecutor workers;
    private final TaskScope taskScope;
    private final ClientCatalogManager catalogManager;
    private final ClientTransferManager transferManager;
    private final ClientAssetRepository assetRepository;
    private final ClientModelRenderTargetManager renderTargetManager;
    private final ClientModelInitialization initialization;
    private final AtomicBoolean closed = new AtomicBoolean();

    private ClientModelService() {
        var threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        workers = new ScheduledThreadPoolExecutor(threadCount, runnable -> {
            var thread = new Thread(runnable, "YSM Client Model Worker");
            thread.setDaemon(true);
            return thread;
        });
        workers.setRemoveOnCancelPolicy(true);
        taskScope = TaskScope.create(workers);
        var system = ModelRuntime.system();
        var storage = system.storage();
        var paths = storage.paths();
        var shared = storage.cache();
        var remoteCache = new RemoteModelCache(paths, shared);
        var builtins = system.builtins();
        catalogManager = new ClientCatalogManager(
                workers, system.reloadableCatalog(), remoteCache, builtins.snapshot());
        var defaultAnimations = new DefaultAnimationRuntime(system.builtinContract());
        var renderTargetLoader = new ModelRenderTargetLoader(new BakedModelCache(paths, shared),
                new BakedAnimationCache(paths, shared), workers, defaultAnimations);
        transferManager = new ClientTransferManager(workers, catalogManager::enqueue);
        assetRepository = new ClientAssetRepository(
                workers, catalogManager, transferManager, remoteCache);
        renderTargetManager = new ClientModelRenderTargetManager(
                catalogManager, assetRepository, transferManager, renderTargetLoader,
                defaultAnimations, taskScope, workers, builtins::finalizeDefaultResidency);
        catalogManager.setListener(renderTargetManager::catalogChanged);
        workers.scheduleAtFixedRate(this::cleanUp, 5, 5, TimeUnit.SECONDS);
        var phases = renderTargetManager.start(catalogManager.initialize());
        initialization = new ClientModelInitialization(
                phases.defaultReady(), phases.builtinsReady(),
                renderTargetManager::cancelBuiltinInitialization);
    }

    public static synchronized ClientModelService start() {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        STATE = State.INITIALIZING;
        STARTUP_FAILURE = null;
        ClientModelService candidate = null;
        try {
            candidate = new ClientModelService();
            candidate.initialization.awaitDefault(
                    INITIALIZATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            INSTANCE = candidate;
            STATE = State.AVAILABLE;
            return candidate;
        } catch (RuntimeException | Error error) {
            if (candidate != null) {
                candidate.closeResources();
            }
            STARTUP_FAILURE = error.getCause() == null ? error : error.getCause();
            STATE = State.FAILED;
            throw new IllegalStateException(
                    "Failed to start the client model service", error);
        }
    }

    public static ClientModelService instance() {
        var service = INSTANCE;
        if (service == null) {
            throw new IllegalStateException("Client model service is not running");
        }
        return service;
    }

    public static Optional<ClientModelService> current() {
        return Optional.ofNullable(INSTANCE);
    }

    public static State state() {
        return STATE;
    }

    public static Optional<Throwable> startupFailure() {
        return Optional.ofNullable(STARTUP_FAILURE);
    }

    public BuiltinReadiness builtinReadiness() {
        return initialization.builtinReadiness();
    }

    public Optional<Throwable> builtinFailure() {
        return initialization.builtinFailure();
    }

    public void awaitBuiltinReadiness() {
        initialization.awaitBuiltins(
                INITIALIZATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public ClientCatalogSnapshot catalog() {
        return catalogManager.snapshot();
    }

    public boolean contains(ModelHash hash) {
        return catalogManager.contains(hash);
    }

    public Optional<String> findRenderTarget(ModelHash hash,
                                             RenderTargetOuterClass.RenderTargetKind kind,
                                             ResourceLocation entityType) {
        return catalogManager.findRenderTarget(hash, kind, entityType);
    }

    public Optional<String> findDefaultRenderTarget(
            RenderTargetOuterClass.RenderTargetKind kind,
            ResourceLocation entityType) {
        return findRenderTarget(renderTargetManager.defaultRenderTarget().modelHash(), kind, entityType);
    }

    public Optional<ModelHash> resolvePath(String path) {
        return catalogManager.resolvePath(path);
    }

    public String displayPath(ModelHash hash) {
        return catalogManager.displayPath(hash);
    }

    public ModelRenderTarget defaultRenderTarget() {
        return renderTargetManager.defaultRenderTarget();
    }

    public TaskScope openRequestScope() {
        return TaskScope.create(workers);
    }

    public CompletableFuture<ModelRenderTargetLease> acquireDefault(TaskContext context, String targetId) {
        return renderTargetManager.acquireDefault(context, targetId);
    }

    public void serverHandshake() {
        transferManager.connect();
        catalogManager.connect();
    }

    public CompletableFuture<ModelRenderTargetLease> acquire(
            TaskContext context, ModelHash hash, String textureName) {
        return acquire(context, hash, RenderTargetIds.PLAYER, textureName);
    }

    public CompletableFuture<ModelRenderTargetLease> acquire(
            TaskContext context, ModelHash hash, String targetId, String textureName) {
        return renderTargetManager.acquire(context, hash, targetId, textureName);
    }

    public boolean isLoaded(ModelHash hash, String textureName) {
        return isLoaded(hash, RenderTargetIds.PLAYER, textureName);
    }

    public boolean isLoaded(ModelHash hash, String targetId, String textureName) {
        return renderTargetManager.isLoaded(hash, targetId, textureName);
    }

    public void reportActiveModelFailure(ModelHash hash, ModelContentVersion version,
                                         boolean fallbackAvailable) {
        renderTargetManager.reportActiveFailure(hash, version, fallbackAvailable);
    }

    public void reportActiveModelUse(ModelHash hash, ModelContentVersion version,
                                     String targetId, String textureName) {
        renderTargetManager.reportActiveUse(new ModelRenderTargetRequestKey(
                hash, version, targetId, textureName));
    }

    public ClientAssetBatch createAssetBatch(TaskContext context) {
        return new ClientAssetBatch(assetRepository.openBatch(context));
    }

    public CustomTexture createTexture(ImageSource source) {
        return new CustomTexture(source, workers);
    }

    public void receive(NetworkPayload<AssetTransferV0.AssetFragment> fragment) {
        transferManager.receive(fragment);
    }

    public void requestFailed(AssetTransferV0.ModelAssetBatchFailure failure) {
        transferManager.requestFailed(failure);
    }

    public void disconnect() {
        transferManager.disconnect();
        catalogManager.disconnect();
        assetRepository.disconnect();
        renderTargetManager.disconnect();

    }

    public void tick() {
        renderTargetManager.tick();
    }

    public int loadingCount() {
        return renderTargetManager.loadingCount();
    }

    private void cleanUp() {
        assetRepository.cleanUp();
        transferManager.cleanUp();
    }

    @Override
    public void close() {
        synchronized (ClientModelService.class) {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            if (INSTANCE == this) {
                INSTANCE = null;
                STATE = State.CLOSED;
            }
            closeResourcesUnchecked();
        }
    }

    private void closeResources() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        closeResourcesUnchecked();
    }

    private void closeResourcesUnchecked() {
        disconnect();
        assetRepository.close();
        renderTargetManager.close();
        catalogManager.close();
        taskScope.close();
        workers.shutdownNow();
    }

}
