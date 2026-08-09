package com.elfmcys.ysm.client.model.internal.catalog;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.client.model.catalog.ClientCatalogEntry;
import com.elfmcys.ysm.client.model.catalog.ClientCatalogSnapshot;
import com.elfmcys.ysm.client.model.catalog.ModelContentVersion;
import com.elfmcys.ysm.model.catalog.CatalogScanResult;
import com.elfmcys.ysm.model.catalog.CatalogBackingKey;
import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import com.elfmcys.ysm.model.catalog.ReloadableCatalogTransition;
import com.elfmcys.ysm.model.catalog.ReloadableModelCatalog;
import com.elfmcys.ysm.model.catalog.RemoteCatalogDecoder;
import com.elfmcys.ysm.model.catalog.RemoteCatalogSnapshot;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.domain.ModelPackDescriptor;
import com.elfmcys.ysm.model.storage.ModelBackingIdentity;
import com.elfmcys.ysm.model.storage.ModelFileHandle;
import com.elfmcys.ysm.model.storage.RemoteModelCache;
import com.elfmcys.ysm.model.storage.RemoteModelHandle;
import com.elfmcys.ysm.network.NetworkHandler;
import com.elfmcys.ysm.network.message.model.AssetTransferMessages;
import com.elfmcys.ysm.network.message.model.TransferKind;
import mixel.manifest.asset.RenderTargetOuterClass;
import com.elfmcys.ysm.util.ModelIdUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Owns local scanning, remote full/delta application and immutable catalog publication. */
public final class ClientCatalogManager implements AutoCloseable {
    private final Executor workers;
    private final CatalogScanResult builtins;
    private final ReloadableModelCatalog reloadableCatalog;
    private final ReloadableModelCatalog.Subscription localSubscription;
    private final RemoteModelCache remoteCache;
    private final StartupPublication<LocalCatalogState> startupPublication =
            new StartupPublication<>();
    private final BackingRecoveryTracker recoveries = new BackingRecoveryTracker();
    private final AtomicLong nextContentVersion = new AtomicLong(1);
    private final AtomicLong nextCatalogGeneration = new AtomicLong(1);
    private final Object catalogChainLock = new Object();

    private volatile LocalCatalogState local = LocalCatalogState.empty();
    private volatile ClientCatalogSnapshot catalog = ClientCatalogSnapshot.empty();
    private volatile CompletableFuture<Void> catalogChain = CompletableFuture.completedFuture(null);
    private volatile Consumer<CatalogChange> listener = ignored -> { };
    private volatile boolean connected;

    public ClientCatalogManager(Executor workers, ReloadableModelCatalog reloadableCatalog,
                                RemoteModelCache remoteCache,
                                CatalogScanResult builtins) {
        this.workers = Objects.requireNonNull(workers, "workers");
        this.reloadableCatalog = Objects.requireNonNull(reloadableCatalog, "reloadableCatalog");
        this.remoteCache = Objects.requireNonNull(remoteCache, "remoteCache");
        this.builtins = Objects.requireNonNull(builtins, "builtins");
        localSubscription = reloadableCatalog.subscribe(workers, this::applyLocalTransition).join();
    }

    public void setListener(Consumer<CatalogChange> listener) {
        this.listener = listener;
    }

    public CompletableFuture<LocalCatalogState> initialize() {
        var startedAt = System.nanoTime();
        var generation = reloadableCatalog.current().reloadGeneration();
        return localSubscription.awaitApplied(generation)
                .thenCompose(ignored -> CompletableFuture.supplyAsync(this::scanRemoteCache, workers))
                .thenApply(remote -> {
                    var state = startupPublication.promote(
                            pending -> pending.withRemote(remote),
                            pending -> install(pending, null, Set.of(), false));
                    YesSteveModel.LOGGER.info(
                            "Finished initial client model catalog publication models={} packs={} remoteCache={} elapsedMs={}",
                            state.models().size(), state.packs().size(), state.remote().size(),
                            Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
                    return state;
                });
    }

    public ClientCatalogSnapshot snapshot() {
        return catalog;
    }

    /** Rebuilds source offers after the intrinsic default discards its encoded representation. */
    public synchronized void refreshAfterDefaultResidency() {
        if (!startupPublication.live()) {
            throw new IllegalStateException("The client catalog is not live");
        }
        replace(local, catalog.server(), Set.of(), false);
    }

    public boolean contains(Hash256 hash) {
        return catalog.models().containsKey(hash);
    }

    public Optional<String> findRenderTarget(Hash256 hash,
                                             RenderTargetOuterClass.RenderTargetKind kind,
                                             ResourceLocation entityType) {
        var entry = catalog.find(hash).orElse(null);
        if (entry == null) {
            return Optional.empty();
        }
        for (var target : entry.displayDescriptor().view().getRenderTargets()) {
            if (target.kind() == kind && ModelIdUtil
                    .getEntityIdMatch(target.matches().toArray(String[]::new)).contains(entityType)) {
                return Optional.of(target.id());
            }
        }
        return Optional.empty();
    }

    public Optional<Hash256> resolvePath(String path) {
        return catalog.sources().resolvePath(path);
    }

    public String displayPath(Hash256 hash) {
        return catalog.find(hash)
                .map(ClientCatalogEntry::displayPath)
                .orElse(hash.toString());
    }

    public LocalCatalogState localState() {
        return local;
    }

    public RemoteModelHandle remoteHandle(Hash256 hash) {
        return local.remote().get(hash);
    }

    public void connect() {
        connected = true;
    }

    public void disconnect() {
        connected = false;
        recoveries.clear();
        replace(local.withRemote(Map.of()), null, Set.of(), true);
    }

    public void refreshAfterBackingFailure(Hash256 modelHash,
                                           ModelContentVersion expectedVersion,
                                           ModelBackingIdentity backingIdentity) {
        var backing = recoveries.begin(catalog, modelHash, expectedVersion, backingIdentity)
                .orElse(null);
        if (backing == null) {
            return;
        }
        reloadableCatalog.recoverUntilSettled(backing).exceptionally(error -> {
            YesSteveModel.LOGGER.error(
                    "Model backing recovery stopped before it settled", unwrap(error));
            return null;
        });
    }

    public void enqueue(TransferKind kind, ArrayBuffer data) {
        synchronized (catalogChainLock) {
            catalogChain = catalogChain.handle((ignored, error) -> null)
                    .thenCompose(value -> process(kind, data))
                    .whenComplete((ignored, error) -> data.close());
        }
    }

    public synchronized void rememberRemote(Hash256 hash, RemoteModelHandle handle) {
        if (!connected) {
            return;
        }
        var updated = new HashMap<>(local.remote());
        updated.put(hash, handle);
        local = local.withRemote(Map.copyOf(updated));
    }

    private CompletableFuture<Void> process(TransferKind kind, ArrayBuffer data) {
        var startedAt = System.nanoTime();
        if (!connected) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            if (kind == TransferKind.CATALOG_FULL) {
                var next = RemoteCatalogDecoder.decodeList(data);
                return onClientThread(() -> commit(next, local, kind, startedAt))
                        .exceptionally(this::failRemoteCatalog);
            }
            var current = catalog.server();
            if (current == null) {
                requestResync();
                return CompletableFuture.completedFuture(null);
            }
            var next = RemoteCatalogDecoder.applyDelta(current, data);
            return onClientThread(() -> commit(next, local, kind, startedAt))
                    .exceptionally(this::failRemoteCatalog);
        } catch (Throwable error) {
            failRemoteCatalog(error);
            return CompletableFuture.completedFuture(null);
        }
    }

    private Void failRemoteCatalog(Throwable error) {
        YesSteveModel.LOGGER.error("Failed to apply remote model catalog; requesting a full resync",
                unwrap(error));
        requestResync();
        return null;
    }

    private void commit(RemoteCatalogSnapshot server, LocalCatalogState localState,
                        TransferKind kind, long startedAt) {
        if (!connected) {
            return;
        }
        replace(retireRemoteAssociations(localState, catalog.server(), server),
                server, Set.of(), true);
        YesSteveModel.LOGGER.info(
                "Applied remote model catalog: kind={}, models={}, packs={}, revision={}, elapsedMs={}",
                kind, server.models().size(), server.packs().size(), server.revision(),
                Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
    }

    private LocalCatalogState retireRemoteAssociations(LocalCatalogState state,
                                                        RemoteCatalogSnapshot previous,
                                                        RemoteCatalogSnapshot next) {
        if (previous == null) {
            return state;
        }
        var remote = new HashMap<>(state.remote());
        previous.models().forEach((hash, offer) -> {
            var replacement = next.models().get(hash);
            if (replacement != null && replacement.descriptor().descriptorHash()
                    .equals(offer.descriptor().descriptorHash())) {
                return;
            }
            var cached = remote.get(hash);
            if (cached != null && cached.descriptor().descriptorHash()
                    .equals(offer.descriptor().descriptorHash())) {
                remote.remove(hash);
            }
            workers.execute(() -> {
                try {
                    remoteCache.removeMetadata(hash, offer.descriptor().descriptorHash());
                } catch (IOException error) {
                    YesSteveModel.LOGGER.debug(
                            "Failed to retire remote model metadata hash={} descriptor={}",
                            hash, offer.descriptor().descriptorHash(), error);
                }
            });
        });
        return state.withRemote(Map.copyOf(remote));
    }

    private void requestResync() {
        NetworkHandler.sendToServer(AssetTransferMessages.resync(
                catalog.serverCursor().orElse(null)));
    }

    private synchronized void install(LocalCatalogState localState, RemoteCatalogSnapshot server,
                                      Set<ModelBackingIdentity> forcedVersions, boolean notify) {
        var mergedRemote = new HashMap<>(localState.remote());
        mergedRemote.putAll(local.remote());
        replace(localState.withRemote(Map.copyOf(mergedRemote)), server, forcedVersions, notify);
    }

    private synchronized void replace(LocalCatalogState localState, RemoteCatalogSnapshot server,
                                      Set<ModelBackingIdentity> forcedVersions, boolean notify) {
        var previous = catalog;
        local = localState;
        catalog = ClientCatalogSnapshot.merge(previous, nextCatalogGeneration.getAndIncrement(),
                localState.models(), localState.packs(), server, forcedVersions,
                nextContentVersion::getAndIncrement);
        if (notify) {
            listener.accept(new CatalogChange(previous, catalog));
        }
    }

    private Map<Hash256, RemoteModelHandle> scanRemoteCache() {
        try {
            return remoteCache.scan();
        } catch (IOException error) {
            YesSteveModel.LOGGER.error("Failed to scan the shared remote model cache", error);
            return Map.of();
        }
    }

    private CompletableFuture<Void> applyLocalTransition(ReloadableCatalogTransition transition) {
        var models = new LinkedHashMap<Hash256, ModelFileHandle>();
        builtins.models().forEach(
                handle -> models.put(handle.descriptor().modelHash(), handle));
        models.putAll(transition.current().models());
        var packs = new java.util.ArrayList<>(builtins.packs());
        packs.addAll(transition.current().packs());
        var state = new LocalCatalogState(Map.copyOf(models), List.copyOf(packs), Map.of());
        var disposition = startupPublication.stage(state);
        if (disposition != StartupPublication.Disposition.LIVE) {
            return CompletableFuture.completedFuture(null);
        }

        return onClientThread(() -> {
            if (!startupPublication.live()) {
                return;
            }
            var forced = recoveries.applyTransition(transition, catalog);
            install(state.withRemote(local.remote()), catalog.server(), forced, true);
        });
    }

    private static CompletableFuture<Void> onClientThread(Runnable action) {
        var result = new CompletableFuture<Void>();
        Minecraft.getInstance().execute(() -> {
            try {
                action.run();
                result.complete(null);
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        });
        return result;
    }

    private static Throwable unwrap(Throwable error) {
        var current = error;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    public record CatalogChange(ClientCatalogSnapshot previous, ClientCatalogSnapshot current) {
    }

    static final class BackingRecoveryTracker {
        private final Set<PendingRecovery> pending = new HashSet<>();

        synchronized Optional<CatalogBackingKey> begin(
                ClientCatalogSnapshot catalog, Hash256 modelHash,
                ModelContentVersion expectedVersion, ModelBackingIdentity backingIdentity) {
            Objects.requireNonNull(catalog, "catalog");
            Objects.requireNonNull(modelHash, "modelHash");
            Objects.requireNonNull(expectedVersion, "expectedVersion");
            Objects.requireNonNull(backingIdentity, "backingIdentity");
            if (backingIdentity.kind() == ModelBackingIdentity.Kind.RESIDENT_DEFAULT
                    || backingIdentity.kind() == ModelBackingIdentity.Kind.REMOTE_SESSION) {
                return Optional.empty();
            }
            var entry = catalog.models().get(modelHash);
            if (entry == null || entry.local() == null
                    || entry.local().location().rootKind() == CatalogRootKind.BUILTIN
                    || !entry.contentVersion().equals(expectedVersion)
                    || !entry.backingIdentity().equals(backingIdentity)) {
                return Optional.empty();
            }
            var backing = new CatalogBackingKey(entry.local().location(), backingIdentity);
            return pending.add(new PendingRecovery(modelHash, expectedVersion, backing))
                    ? Optional.of(backing) : Optional.empty();
        }

        synchronized Set<ModelBackingIdentity> applyTransition(
                ReloadableCatalogTransition transition, ClientCatalogSnapshot catalog) {
            var forced = new HashSet<ModelBackingIdentity>();
            transition.touchedDirectBackings().forEach(key -> forced.add(key.identity()));
            for (var backing : transition.revalidatedBackings()) {
                var matching = pending.stream()
                        .filter(value -> value.backing().equals(backing)).toList();
                matching.forEach(pending::remove);
                for (var recovery : matching) {
                    var entry = catalog.models().get(recovery.modelHash());
                    if (matches(entry, recovery)) {
                        forced.add(backing.identity());
                    }
                }
            }
            pending.removeIf(value -> transition.current().find(value.backing()).isEmpty());
            return Set.copyOf(forced);
        }

        synchronized void clear() {
            pending.clear();
        }

        synchronized int pendingCount() {
            return pending.size();
        }

        private static boolean matches(ClientCatalogEntry entry, PendingRecovery recovery) {
            return entry != null && entry.local() != null
                    && entry.contentVersion().equals(recovery.expectedVersion())
                    && entry.backingIdentity().equals(recovery.backing().identity())
                    && entry.local().location().equals(recovery.backing().location());
        }

        private record PendingRecovery(Hash256 modelHash,
                                       ModelContentVersion expectedVersion,
                                       CatalogBackingKey backing) {
        }
    }

    @Override
    public void close() {
        startupPublication.close();
        localSubscription.close();
    }

    public record LocalCatalogState(Map<Hash256, ModelFileHandle> models,
                                    List<ModelPackDescriptor> packs,
                                    Map<Hash256, RemoteModelHandle> remote) {
        private LocalCatalogState withRemote(Map<Hash256, RemoteModelHandle> value) {
            return new LocalCatalogState(models, packs, value);
        }

        private static LocalCatalogState empty() {
            return new LocalCatalogState(Map.of(), List.of(), Map.of());
        }
    }
}
