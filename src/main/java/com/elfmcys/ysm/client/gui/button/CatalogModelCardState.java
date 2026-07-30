package com.elfmcys.ysm.client.gui.button;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.client.animation.AnimationRegister;
import com.elfmcys.ysm.client.gui.CustomGuiPlayerEntity;
import com.elfmcys.ysm.client.model.ClientAssetBatch;
import com.elfmcys.ysm.client.model.ModelRenderTarget;
import com.elfmcys.ysm.client.model.catalog.CatalogModelMetadata;
import com.elfmcys.ysm.client.model.catalog.ClientCatalogEntry;
import com.elfmcys.ysm.client.model.ClientModelService;
import com.elfmcys.ysm.client.model.ModelRenderTargetLease;
import com.elfmcys.ysm.client.texture.CustomTexture;
import com.elfmcys.ysm.client.texture.CustomTextureManager;
import com.elfmcys.ysm.client.texture.TextureHolder;
import com.elfmcys.ysm.natives.image.ImageSource;
import com.elfmcys.ysm.model.source.ModelAssetSelector;
import com.elfmcys.ysm.task.TaskContext;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

final class CatalogModelCardState implements AutoCloseable {
    private final ClientModelService service = ClientModelService.instance();
    private final TaskContext context;
    private final ClientAssetBatch assets;
    private final ClientCatalogEntry entry;
    private final CatalogModelMetadata metadata;
    private final CustomGuiPlayerEntity entity;
    private final CatalogModelPreviewAnimationState previewAnimations =
            new CatalogModelPreviewAnimationState();

    @Nullable
    private ModelRenderTargetLease lease;
    @Nullable
    private ModelRenderTarget renderTarget;
    @Nullable
    private CustomTexture previewTexture;
    @Nullable
    private CustomTexture backgroundTexture;
    @Nullable
    private CustomTexture foregroundTexture;
    @Nullable
    private TextureHolder preview;
    @Nullable
    private TextureHolder background;
    @Nullable
    private TextureHolder foreground;
    @Nullable
    private Throwable loadError;
    private boolean closed;

    CatalogModelCardState(TaskContext context, ClientAssetBatch assets, ClientCatalogEntry entry,
                          CatalogModelMetadata metadata, CustomGuiPlayerEntity entity) {
        this.context = context;
        this.assets = assets;
        this.entry = entry;
        this.metadata = metadata;
        this.entity = entity;
        startLoading();
    }

    @Nullable
    ModelRenderTarget renderTarget() {
        return renderTarget;
    }

    @Nullable
    TextureHolder preview() {
        return preview;
    }

    @Nullable
    TextureHolder background() {
        return background;
    }

    @Nullable
    TextureHolder foreground() {
        return foreground;
    }

    @Nullable
    Throwable loadError() {
        if (lease != null && !lease.isCurrent()) {
            lease.close();
            lease = null;
            renderTarget = null;
            loadError = new IllegalStateException("The model content changed while this card was open");
        }
        if (loadError == null) {
            loadError = textureFailure(previewTexture);
        }
        if (loadError == null) {
            loadError = textureFailure(backgroundTexture);
        }
        if (loadError == null) {
            loadError = textureFailure(foregroundTexture);
        }
        if (loadError == null && renderTarget != null && renderTarget.playerResources() != null) {
            var resources = renderTarget.playerResources();
            if (resources.defaultVariant().texture() instanceof CustomTexture texture) {
                loadError = texture.failure().orElse(null);
            }
            if (loadError == null && (resources.animations().hasFailures()
                    || resources.fpArmAnimations().hasFailures())) {
                loadError = new IllegalStateException("One or more preview animations failed to load");
            }
        }
        return loadError;
    }

    void updatePreviewAnimations(boolean hovered, boolean focused, long now) {
        if (renderTarget != null) {
            previewAnimations.apply(entity.getPreviewInfo(), hovered, focused, now);
        }
    }

    private static @Nullable Throwable textureFailure(@Nullable CustomTexture texture) {
        return texture == null ? null : texture.failure().orElse(null);
    }

    private void startLoading() {
        requestGuiAssets();
        if (service.isLoaded(entry.modelHash(), metadata.defaultTexture())) {
            requestRenderTarget();
            return;
        }
        assets.preview(entry.modelHash()).whenComplete((data, error) -> Minecraft.getInstance().execute(() -> {
            if (closed) {
                return;
            }
            if (data != null) {
                previewTexture = service.createTexture(data);
                preview = CustomTextureManager.register(previewTexture, true, 10 * 20);
            } else if (!isCancellation(error)) {
                loadError = unwrap(error);
            }
            if (entry.locallyAvailable()) {
                requestRenderTarget();
            }
        }));
    }

    private void requestRenderTarget() {
        service.acquire(context, entry.modelHash(), metadata.defaultTexture())
                .whenComplete((nextLease, error) -> Minecraft.getInstance().execute(() -> {
                    if (nextLease == null) {
                        if (!isCancellation(error)) {
                            loadError = unwrap(error);
                        }
                        return;
                    }
                    if (closed) {
                        nextLease.close();
                        return;
                    }
                    applyRenderTarget(nextLease, nextLease.renderTarget());
                }));
    }

    private void applyRenderTarget(@Nullable ModelRenderTargetLease nextLease, ModelRenderTarget nextRenderTarget) {
        if (lease != null) {
            lease.close();
        }
        lease = nextLease;
        renderTarget = nextRenderTarget;
        entity.reset();
        entity.updateModelAndTexture(entry.modelHash(), metadata.defaultTexture());
        var playerResources = Objects.requireNonNull(nextRenderTarget.playerResources(),
                "Catalog model card requires a player render target");
        var animations = playerResources.animations();
        previewAnimations.configure(nextRenderTarget.info().properties().previewAnimation(),
                animations.containsKey(AnimationRegister.HOVER),
                animations.containsKey(AnimationRegister.HOVER_FADEOUT),
                () -> {
                    var fadeout = animations.get(AnimationRegister.HOVER_FADEOUT);
                    return fadeout == null ? 0 : fadeout.animationLength * 50;
                },
                animations.containsKey(AnimationRegister.FOCUS));
    }

    private void requestGuiAssets() {
        var settings = entry.displayDescriptor().view().getManifest().getInfo().getSettings();
        if (settings.hasGuiBackground()) {
            requestGuiAsset(ModelAssetSelector.PresentationAsset.GUI_BACKGROUND, false);
        }
        if (settings.hasGuiForeground()) {
            requestGuiAsset(ModelAssetSelector.PresentationAsset.GUI_FOREGROUND, true);
        }
    }

    private void requestGuiAsset(ModelAssetSelector.PresentationAsset asset, boolean foregroundAsset) {
        assets.presentation(entry.modelHash(), asset, 0)
                .thenAccept(source -> Minecraft.getInstance().execute(() ->
                        applyPresentationImage(source, foregroundAsset)))
                .exceptionally(error -> {
                    if (!isCancellation(error)) {
                        YesSteveModel.LOGGER.debug("Failed to load GUI presentation asset for {}",
                                entry.modelHash(), unwrap(error));
                    }
                    return null;
                });
    }

    private void applyPresentationImage(ImageSource source, boolean foregroundAsset) {
        if (closed) {
            return;
        }
        var texture = service.createTexture(source);
        var holder = CustomTextureManager.register(texture, true, 10 * 20);
        if (foregroundAsset) {
            foregroundTexture = texture;
            foreground = holder;
        } else {
            backgroundTexture = texture;
            background = holder;
        }
    }

    @Override
    public void close() {
        closed = true;
        entity.reset();
        previewAnimations.reset();
        if (lease != null) {
            lease.close();
            lease = null;
        }
        previewTexture = release(previewTexture);
        backgroundTexture = release(backgroundTexture);
        foregroundTexture = release(foregroundTexture);
        preview = null;
        background = null;
        foreground = null;
        renderTarget = null;
    }

    private static @Nullable CustomTexture release(@Nullable CustomTexture texture) {
        if (texture != null) {
            CustomTextureManager.release(texture);
        }
        return null;
    }

    private static Throwable unwrap(@Nullable Throwable error) {
        if (error == null) {
            return new IllegalStateException("Unknown model load failure");
        }
        while ((error instanceof CompletionException || error instanceof ExecutionException)
                && error.getCause() != null) {
            error = error.getCause();
        }
        return Objects.requireNonNull(error);
    }

    private static boolean isCancellation(@Nullable Throwable error) {
        return error != null && unwrap(error) instanceof CancellationException;
    }
}
