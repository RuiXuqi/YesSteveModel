package com.elfmcys.ysm.client.model.internal.asset;

import com.elfmcys.ysm.client.model.catalog.ClientCatalogEntry;
import com.elfmcys.ysm.client.model.catalog.ClientCatalogSnapshot;
import com.elfmcys.ysm.client.model.internal.catalog.ClientCatalogManager;
import com.elfmcys.ysm.client.model.internal.transfer.ClientTransferManager;
import com.elfmcys.ysm.format.container.AssetContainerView;
import com.elfmcys.ysm.format.schema.file.ChunkDataSource;
import com.elfmcys.ysm.format.schema.file.ChunkImageSource;
import com.elfmcys.ysm.format.schema.file.FileImageSource;
import com.elfmcys.ysm.model.cache.ScopedIdleValueCache;
import com.elfmcys.ysm.model.catalog.ModelCatalogSources;
import com.elfmcys.ysm.model.domain.ModelDescriptor;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.domain.ModelPackDescriptor;
import com.elfmcys.ysm.network.message.model.ModelAssetPlan;
import com.elfmcys.ysm.network.message.model.ReceivedModelAssets;
import com.elfmcys.ysm.model.source.ModelAssetSelector;
import com.elfmcys.ysm.model.source.ModelAssetSubject;
import com.elfmcys.ysm.model.source.PackOffer;
import com.elfmcys.ysm.model.source.SourceId;
import com.elfmcys.ysm.model.storage.ModelFileHandle;
import com.elfmcys.ysm.model.storage.RemoteModelCache;
import com.elfmcys.ysm.model.storage.RemoteModelHandle;
import com.elfmcys.ysm.natives.image.ImageSource;
import com.elfmcys.ysm.task.TaskContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Resolves local and remote presentation/chunk data and owns short-lived asset caches. */
public final class ClientAssetRepository implements AutoCloseable {
    private final Executor workers;
    private final ClientCatalogManager catalogs;
    private final ClientTransferManager transfers;
    private final RemoteModelCache remoteCache;
    private final ScopedIdleValueCache<PreviewKey, ImageSource> previews =
            new ScopedIdleValueCache<>(Duration.ofSeconds(30), ignored -> { });
    private final ScopedIdleValueCache<PackCoverKey, ImageSource> packCovers =
            new ScopedIdleValueCache<>(Duration.ofSeconds(30), ignored -> { });
    private final ScopedIdleValueCache<PresentationKey, ImageSource> presentations =
            new ScopedIdleValueCache<>(Duration.ofSeconds(30), ignored -> { });

    public ClientAssetRepository(Executor workers, ClientCatalogManager catalogs,
                          ClientTransferManager transfers, RemoteModelCache remoteCache) {
        this.workers = workers;
        this.catalogs = catalogs;
        this.transfers = transfers;
        this.remoteCache = remoteCache;
    }

    public Batch openBatch(TaskContext context) {
        var snapshot = catalogs.snapshot();
        return new Batch(context, snapshot,
                transfers.openBatch(snapshot.serverCursor().orElse(null)));
    }

    private CompletableFuture<ImageSource> preview(Batch batch, ModelHash hash) {
        var entry = batch.snapshot.find(hash).orElse(null);
        if (entry == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Unknown model hash: " + hash));
        }
        var descriptor = entry.displayDescriptor();
        return previews.get(batch.context, new PreviewKey(hash, descriptor.descriptorHash()), (loadContext, ignored) ->
                entry.local() != null
                        ? localPreview(entry.local())
                        : remoteImage(loadContext, batch.transfers, entry, ModelAssetSelector.preview()));
    }

    private CompletableFuture<ImageSource> packCover(Batch batch, PackOffer pack) {
        if (pack.coverHash() == null) {
            return CompletableFuture.failedFuture(new IOException("Model pack contains no cover"));
        }
        var key = new PackCoverKey(pack.sourceId(), pack.subject(), pack.coverHash());
        return packCovers.get(batch.context, key, (loadContext, ignored) -> {
            var localPack = catalogs.localState().packs().stream()
                    .filter(value -> value.rootKind().namespace().equals(pack.subject().namespace())
                            && value.hierarchy().equals(pack.subject().hierarchy())
                            && pack.coverHash().matches(value.coverHash()))
                    .findFirst().orElse(null);
            if (localPack != null) {
                return localPackCover(localPack);
            }
            var chunk = packCoverChunk(pack);
            var required = CompletableFuture.supplyAsync(() -> !remoteCache.hasChunk(chunk), workers);
            return batch.transfers.requestPack(loadContext,
                            pack.subject(),
                            (ModelAssetSelector.PackCover) ModelAssetSelector.packCover(pack.coverHash()),
                            required)
                    .thenApply(bundle -> {
                        if (bundle != null) {
                            try (bundle) {
                                remoteCache.storeChunks(bundle);
                                if (!remoteCache.hasChunk(chunk)) {
                                    throw new IOException("Cached pack cover does not match the catalog");
                                }
                            } catch (IOException error) {
                                throw new UncheckedIOException(error);
                            }
                        }
                        return ChunkImageSource.probed(
                                remoteCache.chunks(), chunk, pack.coverFormat());
                    });
        });
    }

    private CompletableFuture<ImageSource> presentation(Batch batch, ModelHash hash,
                                                        ModelAssetSelector.PresentationAsset asset, int index) {
        var entry = batch.snapshot.find(hash).orElse(null);
        if (entry == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Unknown model hash: " + hash));
        }
        var descriptor = entry.displayDescriptor();
        var selector = ModelAssetSelector.presentation(asset, index);
        var key = new PresentationKey(hash, descriptor.descriptorHash(), asset, index);
        return presentations.get(batch.context, key, (loadContext, ignored) -> entry.local() != null
                ? localPresentation(entry.local(), selector)
                : remoteImage(loadContext, batch.transfers, entry, selector));
    }

    public CompletableFuture<RemoteModelHandle> ensureRemote(TaskContext context, ClientCatalogEntry entry,
                                                       String targetId, String texture) {
        var descriptor = Objects.requireNonNull(entry.server(), "server offer").descriptor();
        var complete = ModelAssetSelector.renderTarget(targetId, texture, EnumSet.of(
                ModelAssetSelector.RenderTargetComponent.DEFINITION,
                ModelAssetSelector.RenderTargetComponent.TEXTURE_SET,
                ModelAssetSelector.RenderTargetComponent.COMMON_BEHAVIOR));
        var textureOnly = ModelAssetSelector.renderTarget(targetId, texture, EnumSet.of(
                ModelAssetSelector.RenderTargetComponent.TEXTURE_SET));
        var definition = ModelAssetSelector.renderTarget(targetId, "", EnumSet.of(
                ModelAssetSelector.RenderTargetComponent.DEFINITION,
                ModelAssetSelector.RenderTargetComponent.COMMON_BEHAVIOR));
        return CompletableFuture.supplyAsync(() -> {
            var cached = catalogs.remoteHandle(entry.modelHash());
            var sameRepresentation = cached != null
                    && cached.descriptor().sameRepresentation(descriptor);
            if (sameRepresentation && ModelAssetPlan.isCached(
                    descriptor, complete, remoteCache::hasChunk)) {
                return new RemoteLoadPlan(cached, null);
            }
            return new RemoteLoadPlan(cached,
                    sameRepresentation && ModelAssetPlan.isCached(
                            descriptor, definition, remoteCache::hasChunk)
                            ? textureOnly : complete);
        }, workers).thenCompose(plan -> {
            if (plan.selector() == null) {
                return CompletableFuture.completedFuture(plan.cached());
            }
            return request(context, entry, plan.selector()).thenApply(bundle -> {
                try (bundle) {
                    remoteCache.storeChunks(bundle);
                    var requested = (ModelAssetSelector.RenderTarget) plan.selector();
                    var handle = requested.components().contains(
                            ModelAssetSelector.RenderTargetComponent.DEFINITION)
                            ? remoteCache.storeMetadata(descriptor) : plan.cached();
                    if (handle == null) {
                        throw new IOException(
                                "Texture response arrived without cached model metadata");
                    }
                    catalogs.rememberRemote(entry.modelHash(), handle);
                    return handle;
                } catch (IOException error) {
                    throw new UncheckedIOException(error);
                }
            });
        });
    }

    public void cleanUp() {
        previews.cleanUp();
        packCovers.cleanUp();
        presentations.cleanUp();
    }

    public void disconnect() {
        previews.close();
        packCovers.close();
        presentations.close();
    }

    private CompletableFuture<ReceivedModelAssets> request(
            TaskContext context, ClientCatalogEntry entry, ModelAssetSelector selector) {
        return transfers.request(context, entry, selector,
                catalogs.snapshot().serverCursor().orElse(null));
    }

    private static CompletableFuture<ImageSource> localPreview(ModelFileHandle handle) {
        try {
            var source = handle.view().thumbnailSource(handle.chunks());
            return source == null
                    ? CompletableFuture.failedFuture(new IOException("Model contains no preview image"))
                    : CompletableFuture.completedFuture(source);
        } catch (IOException | RuntimeException error) {
            return CompletableFuture.failedFuture(error);
        }
    }

    private static CompletableFuture<ImageSource> localPresentation(ModelFileHandle handle,
                                                                     ModelAssetSelector selector) {
        try {
            return CompletableFuture.completedFuture(imageSource(handle.descriptor(), handle.chunks(), selector));
        } catch (IOException | RuntimeException error) {
            return CompletableFuture.failedFuture(error);
        }
    }

    private CompletableFuture<ImageSource> remoteImage(
            TaskContext context, ClientTransferManager.AssetBatchSession batch,
            ClientCatalogEntry entry,
            ModelAssetSelector selector) {
        var descriptor = Objects.requireNonNull(entry.server(), "server offer").descriptor();
        var required = CompletableFuture.supplyAsync(() -> !ModelAssetPlan.isCached(
                descriptor, selector, remoteCache::hasChunk), workers);
        return batch.request(context, entry, selector, required)
                .thenApply(bundle -> {
                    if (bundle != null) {
                            try (bundle) {
                                remoteCache.storeChunks(bundle);
                                if (!ModelAssetPlan.isCached(descriptor, selector, remoteCache::hasChunk)) {
                                    throw new IOException("Cached model image does not match the descriptor");
                                }
                            } catch (IOException error) {
                                throw new UncheckedIOException(error);
                            }
                    }
                    try {
                        return imageSource(descriptor, remoteCache.chunks(), selector);
                    } catch (IOException error) {
                        throw new UncheckedIOException(error);
                    }
                });
    }

    private static ImageSource imageSource(ModelDescriptor descriptor, ChunkDataSource chunks,
                                           ModelAssetSelector selector) throws IOException {
        var view = descriptor.view();
        if (selector instanceof ModelAssetSelector.ModelPreview) {
            var source = view.thumbnailSource(chunks);
            if (source == null) {
                throw new IOException("Model contains no preview image");
            }
            return source;
        }
        if (!(selector instanceof ModelAssetSelector.ModelPresentation presentation)) {
            throw new IOException("Selector does not identify a model image");
        }
        var info = view.getManifest().getInfo();
        return switch (presentation.asset()) {
            case MODEL_ICON -> {
                var source = view.iconSource(chunks);
                if (source == null) {
                    throw new IOException("Model contains no icon");
                }
                yield source;
            }
            case AUTHOR_AVATAR -> {
                if (!info.hasMetadata() || !info.getMetadata().hasAuthors()
                        || presentation.index() >= info.getMetadata().getAuthors().length()
                        || !info.getMetadata().getAuthors().get(presentation.index()).hasAvatar()) {
                    throw new IOException("Model contains no requested author avatar");
                }
                yield view.getFileView().imageBlobSource(chunks,
                        info.getMetadata().getAuthors().get(presentation.index()).getAvatar());
            }
            case GUI_FOREGROUND -> {
                if (!info.getSettings().hasGuiForeground()) {
                    throw new IOException("Model contains no GUI foreground");
                }
                yield view.getFileView().imageBlobSource(chunks, info.getSettings().getGuiForeground());
            }
            case GUI_BACKGROUND -> {
                if (!info.getSettings().hasGuiBackground()) {
                    throw new IOException("Model contains no GUI background");
                }
                yield view.getFileView().imageBlobSource(chunks, info.getSettings().getGuiBackground());
            }
        };
    }

    private static CompletableFuture<ImageSource> localPackCover(ModelPackDescriptor pack) {
        try {
            var root = ModelCatalogSources.sources().stream()
                    .filter(value -> value.rootKind() == pack.rootKind())
                    .findFirst().orElseThrow(() -> new IOException("Pack root is unavailable"));
            var hierarchy = pack.hierarchy().endsWith("/")
                    ? pack.hierarchy().substring(0, pack.hierarchy().length() - 1)
                    : pack.hierarchy();
            var rootPath = root.path().toAbsolutePath().normalize();
            var file = rootPath.resolve(hierarchy).resolve("ysm-pack.png").normalize();
            if (!file.startsWith(rootPath)) {
                throw new IOException("Pack cover is unavailable");
            }
            return CompletableFuture.completedFuture(new FileImageSource(
                    file, pack.coverSize(), pack.coverHash(), pack.coverFormat()));
        } catch (IOException | RuntimeException error) {
            return CompletableFuture.failedFuture(error);
        }
    }

    private static AssetContainerView.ChunkInfo packCoverChunk(PackOffer pack) {
        return new AssetContainerView.ChunkInfo("pack-cover", pack.coverFormat(), 0, pack.coverSize(),
                pack.coverSize(), 0, 0, 0, pack.coverHash().bytes());
    }

    @Override
    public void close() {
        disconnect();
    }

    private record PreviewKey(ModelHash modelHash, ModelHash descriptorHash) {
    }

    private record PackCoverKey(SourceId sourceId, ModelAssetSubject.Pack subject, ModelHash hash) {
    }

    private record PresentationKey(ModelHash modelHash, ModelHash descriptorHash,
                                   ModelAssetSelector.PresentationAsset asset, int index) {
    }

    private record RemoteLoadPlan(RemoteModelHandle cached, ModelAssetSelector selector) {
    }

    public final class Batch {
        private final TaskContext context;
        private final ClientCatalogSnapshot snapshot;
        private final ClientTransferManager.AssetBatchSession transfers;
        private boolean submitted;

        private Batch(TaskContext context, ClientCatalogSnapshot snapshot,
                      ClientTransferManager.AssetBatchSession transfers) {
            this.context = context;
            this.snapshot = snapshot;
            this.transfers = transfers;
        }

        public synchronized CompletableFuture<ImageSource> preview(ModelHash hash) {
            checkRegistrationOpen();
            return ClientAssetRepository.this.preview(this, hash);
        }

        public synchronized CompletableFuture<ImageSource> packCover(PackOffer pack) {
            checkRegistrationOpen();
            return ClientAssetRepository.this.packCover(this, pack);
        }

        public synchronized CompletableFuture<ImageSource> presentation(
                ModelHash hash, ModelAssetSelector.PresentationAsset asset, int index) {
            checkRegistrationOpen();
            return ClientAssetRepository.this.presentation(this, hash, asset, index);
        }

        public synchronized void submit() {
            checkRegistrationOpen();
            submitted = true;
            transfers.submit();
        }

        private void checkRegistrationOpen() {
            if (submitted) {
                throw new IllegalStateException("Asset batch was already submitted");
            }
        }
    }
}
