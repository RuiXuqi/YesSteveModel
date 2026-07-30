package com.elfmcys.ysm.client.model.internal.render;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.client.model.ModelRenderTarget;
import com.elfmcys.ysm.client.model.ModelRenderTargetLease;
import com.elfmcys.ysm.client.model.ModelResourceFailures;
import com.elfmcys.ysm.client.model.catalog.ClientCatalogEntry;
import com.elfmcys.ysm.client.model.catalog.ModelContentVersion;
import com.elfmcys.ysm.client.model.internal.asset.ClientAssetRepository;
import com.elfmcys.ysm.client.model.internal.catalog.ClientCatalogManager;
import com.elfmcys.ysm.client.model.internal.transfer.ClientTransferManager;
import com.elfmcys.ysm.config.ClientConfig;
import com.elfmcys.ysm.model.domain.ModelDescriptor;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import com.elfmcys.ysm.model.domain.RenderTargetIds;
import com.elfmcys.ysm.model.catalog.DefaultAnimationKey;
import com.elfmcys.ysm.network.message.model.ModelAssetPlan;
import com.elfmcys.ysm.model.storage.ModelFileHandle;
import com.elfmcys.ysm.model.storage.ModelBackingException;
import com.elfmcys.ysm.task.TaskContext;
import com.elfmcys.ysm.task.TaskScope;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Objects;

/** Owns render-target loading, lease retention, builtin initialization and catalog invalidation. */
public final class ClientModelRenderTargetManager implements AutoCloseable {
    private static final int PRELOAD_LIMIT = 50;

    private final ClientCatalogManager catalogs;
    private final Runnable finalizeDefaultResidency;
    private final ClientAssetRepository assets;
    private final ClientTransferManager transfers;
    private final ModelRenderTargetLoader loader;
    private final DefaultAnimationRuntime defaultAnimations;
    private final TaskContext serviceContext;
    private final TaskScope builtinInitializationScope;
    private final ModelFailureRegistry failures = new ModelFailureRegistry();
    private final ModelRenderTargetCache renderTargets = new ModelRenderTargetCache(failures);
    private final ModelFailureNotificationAggregator notifications;
    private final FinalPublicationGate defaultPublication = new FinalPublicationGate();

    private volatile CompletableFuture<Void> defaultInitialization;
    private volatile ModelRenderTarget defaultRenderTarget;
    private volatile boolean preloadAllSession;
    private volatile boolean pinAllSession;

    public ClientModelRenderTargetManager(ClientCatalogManager catalogs, ClientAssetRepository assets,
                         ClientTransferManager transfers, ModelRenderTargetLoader loader,
                         DefaultAnimationRuntime defaultAnimations,
                         TaskContext serviceContext, ScheduledExecutorService scheduler,
                         Runnable finalizeDefaultResidency) {
        this.catalogs = catalogs;
        this.finalizeDefaultResidency = Objects.requireNonNull(
                finalizeDefaultResidency, "finalizeDefaultResidency");
        this.assets = assets;
        this.transfers = transfers;
        this.loader = loader;
        this.defaultAnimations = defaultAnimations;
        this.serviceContext = serviceContext;
        notifications = new ModelFailureNotificationAggregator(scheduler);
        builtinInitializationScope = serviceContext.derive();
    }

    public synchronized Initialization start(
            CompletableFuture<ClientCatalogManager.LocalCatalogState> localInitialization) {
        if (defaultInitialization != null) {
            throw new IllegalStateException("Builtin initialization has already started");
        }
        var defaultAndCatalog = localInitialization.thenCompose(state ->
                initializeDefault(state).thenApply(ignored -> state));
        defaultInitialization = defaultAndCatalog.thenAccept(ignored -> {
        });
        defaultInitialization.whenComplete((ignored, error) -> {
            if (error != null) {
                YesSteveModel.LOGGER.error(
                        "Failed to initialize the builtin default model", unwrap(error));
            }
        });
        var builtinsReady = defaultAndCatalog.thenCompose(this::initializeOptionalBuiltins);
        builtinsReady.whenComplete((ignored, error) -> {
            if (error != null) {
                YesSteveModel.LOGGER.error(
                        "Failed to finish builtin model initialization", unwrap(error));
            }
        });
        return new Initialization(defaultInitialization, builtinsReady);
    }

    public ModelRenderTarget defaultRenderTarget() {
        if (defaultRenderTarget == null) {
            throw new IllegalStateException("The builtin default model is not ready");
        }
        return defaultRenderTarget;
    }

    public CompletableFuture<ModelRenderTargetLease> acquireDefault(TaskContext context, String targetId) {
        return requireDefaultInitialization().thenCompose(ignored -> {
            var renderTarget = defaultRenderTarget();
            var entry = catalogs.snapshot().find(renderTarget.modelHash()).orElseThrow();
            var target = entry.displayDescriptor().view().getRenderTarget(targetId) == null
                    ? RenderTargetIds.PLAYER : targetId;
            return acquire(context, renderTarget.modelHash(), target, "");
        });
    }

    public CompletableFuture<ModelRenderTargetLease> acquire(TaskContext context, ModelHash hash, String targetId,
                                                  String textureName) {
        return requireDefaultInitialization().thenCompose(ignored -> {
            var entry = catalogs.snapshot().find(hash).orElse(null);
            if (entry == null) {
                return CompletableFuture.failedFuture(
                        new IllegalArgumentException("Unknown model hash: " + hash));
            }
            var selected = selectTexture(entry.displayDescriptor(), targetId, textureName);
            var key = new ModelRenderTargetRequestKey(hash, entry.contentVersion(), targetId, selected);
            var retention = retention(entry);
            return renderTargets.acquire(context, key, retention,
                    loadContext -> load(loadContext, key, entry, targetId, selected, retention));
        });
    }

    public boolean isLoaded(ModelHash hash, String targetId, String textureName) {
        var entry = catalogs.snapshot().find(hash).orElse(null);
        if (entry == null) {
            return false;
        }
        return renderTargets.isLoaded(new ModelRenderTargetRequestKey(hash, entry.contentVersion(), targetId,
                selectTexture(entry.displayDescriptor(), targetId, textureName)));
    }

    public void catalogChanged(ClientCatalogManager.CatalogChange change) {
        change.previous().models().forEach((hash, previous) -> {
            var current = change.current().models().get(hash);
            if (current == null || !previous.contentVersion().equals(current.contentVersion())) {
                renderTargets.invalidate(previous.contentVersion());
                failures.clear(previous.contentVersion());
                notifications.recovered(previous.contentVersion());
            }
        });

        var catalog = change.current();
        var count = catalog.models().values().stream().filter(entry -> !entry.builtin()).count();
        var nextPinAllSession = ClientConfig.PIN_ALL_SESSION_MODELS != null
                && ClientConfig.PIN_ALL_SESSION_MODELS.get();
        var nextPreloadAllSession = !nextPinAllSession && count <= PRELOAD_LIMIT;
        if ((pinAllSession && !nextPinAllSession)
                || (preloadAllSession && !nextPreloadAllSession)) {
            renderTargets.demoteSessionRetention();
        }
        pinAllSession = nextPinAllSession;
        preloadAllSession = nextPreloadAllSession;
        if (preloadAllSession || pinAllSession) {
            catalog.models().values().stream()
                    .filter(ClientCatalogEntry::locallyAvailable)
                    .filter(entry -> !entry.builtin())
                    .forEach(this::preload);
        }
    }

    public void disconnect() {
        preloadAllSession = false;
        pinAllSession = false;
        renderTargets.clearSession();
    }

    public void tick() {
        renderTargets.evictIdle();
    }

    public int loadingCount() {
        return renderTargets.loadingCount();
    }

    public void reportActiveFailure(ModelHash hash, ModelContentVersion version,
                                    boolean fallbackAvailable) {
        notifications.report(hash, version, fallbackAvailable
                ? ModelFailureNotificationAggregator.Outcome.FALLBACK
                : ModelFailureNotificationAggregator.Outcome.STOPPED);
    }

    public void reportActiveUse(ModelRenderTargetRequestKey key) {
        var texturePrefix = key.renderTargetId() + "/" + key.textureName() + "/";
        var animationPrefix = key.renderTargetId() + "/";
        if (failures.hasMatching(key.contentVersion(), ModelFailureRegistry.Stage.TEXTURE,
                resource -> resource.startsWith(texturePrefix))
                || failures.hasMatching(key.contentVersion(), ModelFailureRegistry.Stage.ANIMATION,
                resource -> resource.startsWith(animationPrefix))) {
            notifications.report(key.modelHash(), key.contentVersion(),
                    ModelFailureNotificationAggregator.Outcome.PARTIAL);
        }
    }

    private CompletableFuture<ModelRenderTarget> load(TaskContext context, ModelRenderTargetRequestKey key,
                                                   ClientCatalogEntry entry,
                                                   String targetId, String texture,
                                                  ModelRenderTargetCache.Retention retention) {
        CompletableFuture<ModelRenderTarget> result;
        String source;
        if (entry.local() != null) {
            var handle = entry.local();
            source = handle.location().rootKind().namespace();
            result = loader.loadTarget(context, handle.descriptor(), handle.chunks(), targetId, texture,
                    entry.builtin(), false, resourceFailures(key, entry));
        } else {
            source = "game-server";
            result = assets.ensureRemote(context, entry, targetId, texture)
                    .thenCompose(handle -> loader.loadTarget(context, entry.server().descriptor(), handle.chunks(),
                            targetId, texture, false, false, resourceFailures(key, entry)));
        }
        var traced = traceLoad(result, entry.modelHash(), catalogs.displayPath(entry.modelHash()),
                targetId, texture, source, retention);
        traced.whenComplete((ignored, error) -> {
            var backingFailure = findBackingFailure(error);
            if (backingFailure != null
                    && backingFailure.backingIdentity().equals(entry.backingIdentity())) {
                catalogs.refreshAfterBackingFailure(
                        key.modelHash(), key.contentVersion(), entry.backingIdentity());
            }
        });
        return traced;
    }

    private void preload(ClientCatalogEntry entry) {
        entry.displayDescriptor().view().getRenderTargets().forEach(target ->
                acquire(serviceContext, entry.modelHash(), target.id(), "").whenComplete((lease, error) -> {
                    if (lease != null) {
                        lease.close();
                    }
                }));
    }

    private CompletableFuture<Void> initializeDefault(ClientCatalogManager.LocalCatalogState state) {
        var startedAt = System.nanoTime();
        var defaultHandle = state.models().values().stream().filter(this::isDefault).findFirst()
                .orElseThrow(() -> new IllegalStateException("Builtin default model was not found"));
        var defaultLoads = defaultHandle.view().getRenderTargets().stream()
                .flatMap(target -> target.getTextureNames().stream()
                        .map(texture -> loadDefaultTarget(defaultHandle, target.id(), texture)))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(defaultLoads).thenRun(() -> defaultPublication.publish(() -> {
            var loaded = java.util.Arrays.stream(defaultLoads)
                    .map(future -> (DefaultTargetLoad) future.join())
                    .toList();
            var selected = loaded.stream()
                    .filter(load -> load.targetId().equals(RenderTargetIds.PLAYER))
                    .filter(load -> load.texture().equals(
                            selectTexture(defaultHandle.descriptor(), load.targetId(), "")))
                    .findFirst().orElseThrow(() -> new IllegalStateException(
                            "Builtin default model contains no player render target"));
            finalizeDefaultResidency.run();
            catalogs.refreshAfterDefaultResidency();
            for (var load : loaded) {
                publishDefaultAnimations(defaultHandle, load.targetId(), load.renderTarget());
            }
            defaultRenderTarget = selected.renderTarget();
            var elapsed = java.time.Duration.ofNanos(
                    System.nanoTime() - startedAt).toMillis();
            YesSteveModel.LOGGER.info(
                    "Finished builtin default model initialization targets={} elapsedMs={}",
                    loaded.size(), elapsed);
        })).whenComplete((ignored, error) -> closeCompletedDefaultLoads(defaultLoads));
    }

    private CompletableFuture<Void> initializeOptionalBuiltins(
            ClientCatalogManager.LocalCatalogState state) {
        var startedAt = System.nanoTime();
        var failures = new AtomicInteger();
        var optionalLoads = state.models().values().stream()
                .filter(handle -> handle.location().rootKind() == CatalogRootKind.BUILTIN)
                .filter(handle -> !isDefault(handle))
                .flatMap(handle -> handle.view().getRenderTargets().stream()
                        .map(target -> loadOptionalBuiltinTarget(
                                handle, target.id(), failures)))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(optionalLoads).thenRun(() -> {
            var elapsed = java.time.Duration.ofNanos(
                    System.nanoTime() - startedAt).toMillis();
            YesSteveModel.LOGGER.info(
                    "Finished optional builtin model initialization targets={} failures={} elapsedMs={}",
                    optionalLoads.length, failures.get(), elapsed);
        });
    }

    private CompletableFuture<DefaultTargetLoad> loadDefaultTarget(
            ModelFileHandle handle, String targetId, String texture) {
        return loadBuiltinTarget(serviceContext, handle, targetId, texture, true)
                .thenApply(lease -> new DefaultTargetLoad(
                        targetId, texture, lease.renderTarget(), lease));
    }

    private static void closeCompletedDefaultLoads(CompletableFuture<?>[] loads) {
        for (var future : loads) {
            if (!future.isDone() || future.isCompletedExceptionally() || future.isCancelled()) {
                continue;
            }
            var load = (DefaultTargetLoad) future.getNow(null);
            if (load != null) {
                load.lease().close();
            }
        }
    }

    private CompletableFuture<Void> loadOptionalBuiltinTarget(
            ModelFileHandle handle, String targetId, AtomicInteger failures) {
        try {
            return loadBuiltinTarget(builtinInitializationScope, handle, targetId,
                            selectTexture(handle.descriptor(), targetId, ""), false)
                    .thenAccept(ModelRenderTargetLease::close)
                    .exceptionally(error -> {
                        var cause = unwrap(error);
                        if (cause instanceof Error fatal) {
                            throw fatal;
                        }
                        failures.incrementAndGet();
                        logOptionalBuiltinFailure(handle, targetId, cause);
                        return null;
                    });
        } catch (RuntimeException error) {
            failures.incrementAndGet();
            logOptionalBuiltinFailure(handle, targetId, error);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static void logOptionalBuiltinFailure(ModelFileHandle handle, String targetId,
                                                  Throwable error) {
        YesSteveModel.LOGGER.error(
                "Failed to initialize optional builtin model path={} target={}",
                handle.location().path().value(), targetId, unwrap(error));
    }

    private CompletableFuture<ModelRenderTargetLease> loadBuiltinTarget(
            TaskContext context, ModelFileHandle handle, String targetId,
            String texture, boolean defaultModel) {
        var entry = catalogs.snapshot().find(handle.descriptor().modelHash()).orElseThrow();
        var key = new ModelRenderTargetRequestKey(handle.descriptor().modelHash(),
                entry.contentVersion(), targetId, texture);
        return renderTargets.acquire(context,
                        key,
                        ModelRenderTargetCache.Retention.PERMANENT,
                        loadContext -> traceLoad(loader.loadTarget(
                                        loadContext, handle.descriptor(), handle.chunks(),
                                        targetId, texture, true, defaultModel,
                                        defaultModel ? ModelResourceFailures.none()
                                                : resourceFailures(key, entry)),
                                handle.descriptor().modelHash(), handle.location().path().value(),
                                targetId, texture, CatalogRootKind.BUILTIN.namespace(),
                                ModelRenderTargetCache.Retention.PERMANENT));
    }

    private void publishDefaultAnimations(ModelFileHandle handle, String targetId,
                                          ModelRenderTarget renderTarget) {
        var target = handle.view().requireRenderTarget(targetId).descriptor();
        var player = renderTarget.playerResources();
        if (player != null) {
            defaultAnimations.publish(DefaultAnimationKey.domain(target, "main"),
                    player.animations());
            defaultAnimations.publish(DefaultAnimationKey.domain(target, "fp_arm"),
                    player.fpArmAnimations());
            return;
        }
        var projectile = renderTarget.projectileResources();
        if (projectile != null) {
            defaultAnimations.publish(DefaultAnimationKey.domain(target, "main"),
                    projectile.animations());
            return;
        }
        var vehicle = renderTarget.vehicleResources();
        if (vehicle != null) {
            defaultAnimations.publish(DefaultAnimationKey.domain(target, "main"),
                    vehicle.animations());
        }
    }

    private static CompletableFuture<ModelRenderTarget> traceLoad(
            CompletableFuture<ModelRenderTarget> result, ModelHash hash, String path,
            String targetId, String texture, String source,
            ModelRenderTargetCache.Retention retention) {
        var startedAt = System.nanoTime();
        return result.whenComplete((ignored, error) -> {
            var elapsed = java.time.Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            if (error == null) {
                YesSteveModel.LOGGER.debug(
                        "Loaded model render target hash={} path={} target={} texture={} source={} retention={} elapsedMs={}",
                        hash, path, targetId, texture, source, retention, elapsed);
                return;
            }
            var cause = unwrap(error);
            if (!(cause instanceof CancellationException)) {
                YesSteveModel.LOGGER.debug(
                        "Failed to load model render target hash={} path={} target={} texture={} source={} retention={} elapsedMs={}",
                        hash, path, targetId, texture, source, retention, elapsed, cause);
            }
        });
    }

    private record DefaultTargetLoad(String targetId, String texture,
                                     ModelRenderTarget renderTarget,
                                     ModelRenderTargetLease lease) {
    }

    private static Throwable unwrap(Throwable error) {
        var current = error;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static ModelBackingException findBackingFailure(Throwable error) {
        var current = error;
        while (current != null) {
            if (current instanceof ModelBackingException backingFailure) {
                return backingFailure;
            }
            current = current.getCause();
        }
        return null;
    }

    private ModelResourceFailures resourceFailures(ModelRenderTargetRequestKey key,
                                                    ClientCatalogEntry entry) {
        return new ModelResourceFailures() {
            @Override
            public com.elfmcys.ysm.client.model.ModelResourceFailureGate texture(
                    String resource) {
                return gate(ModelFailureRegistry.Stage.TEXTURE, resource);
            }

            @Override
            public com.elfmcys.ysm.client.model.ModelResourceFailureGate animation(
                    String resource) {
                return gate(ModelFailureRegistry.Stage.ANIMATION, resource);
            }

            private com.elfmcys.ysm.client.model.ModelResourceFailureGate gate(
                    ModelFailureRegistry.Stage stage, String resource) {
                return failures.gate(key.contentVersion(), stage, resource,
                        error -> resourceFailed(key, entry, stage, resource, error));
            }
        };
    }

    private void resourceFailed(ModelRenderTargetRequestKey key, ClientCatalogEntry entry,
                                ModelFailureRegistry.Stage stage, String resource,
                                Throwable error) {
        YesSteveModel.LOGGER.debug(
                "Model lazy resource failed hash={} path={} stage={} resource={}",
                entry.modelHash(), catalogs.displayPath(entry.modelHash()), stage, resource, error);
        var backingFailure = findBackingFailure(error);
        if (backingFailure != null
                && backingFailure.backingIdentity().equals(entry.backingIdentity())) {
            catalogs.refreshAfterBackingFailure(
                    key.modelHash(), key.contentVersion(), entry.backingIdentity());
        }
    }

    private ModelRenderTargetCache.Retention retention(ClientCatalogEntry entry) {
        if (entry.builtin()) {
            return ModelRenderTargetCache.Retention.PERMANENT;
        }
        if ((preloadAllSession && entry.locallyAvailable())
                || (pinAllSession && transfers.connected())) {
            return ModelRenderTargetCache.Retention.SESSION;
        }
        return ModelRenderTargetCache.Retention.IDLE;
    }

    private static String selectTexture(ModelDescriptor descriptor, String targetId, String requested) {
        return ModelAssetPlan.chooseTexture(descriptor.view().getManifest(), targetId, requested);
    }

    private boolean isDefault(ModelFileHandle handle) {
        return handle.location().rootKind() == CatalogRootKind.BUILTIN
                && (handle.location().path().value().equals("default")
                || handle.location().path().value().equals("default.mxc"));
    }

    public void cancelBuiltinInitialization() {
        builtinInitializationScope.close();
    }

    private CompletableFuture<Void> requireDefaultInitialization() {
        var initialization = defaultInitialization;
        if (initialization == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Builtin initialization has not started"));
        }
        return initialization;
    }

    @Override
    public void close() {
        if (!defaultPublication.close()) {
            return;
        }
        defaultRenderTarget = null;
        builtinInitializationScope.close();
        defaultAnimations.clear();
        notifications.close();
        renderTargets.close();
        failures.clear();
    }

    public record Initialization(
            CompletableFuture<Void> defaultReady,
            CompletableFuture<Void> builtinsReady) {
    }
}
