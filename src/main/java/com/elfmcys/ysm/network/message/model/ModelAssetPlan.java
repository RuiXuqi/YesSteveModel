package com.elfmcys.ysm.network.message.model;

import com.elfmcys.ysm.format.container.AssetContainerView;
import com.elfmcys.ysm.format.schema.file.AssetFileConstant;
import com.elfmcys.ysm.format.schema.model.ModelFileConstant;
import com.elfmcys.ysm.model.domain.ModelDescriptor;
import com.elfmcys.ysm.model.source.ModelAssetSelector;
import mixel.common.ImageOuterClass;
import mixel.manifest.ManifestOuterClass;
import mixel.manifest.asset.RenderTargetOuterClass;
import mixel.manifest.asset.Texture;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Resolves a validated selector to the minimum set of model-container chunks. */
public final class ModelAssetPlan {
    private ModelAssetPlan() {
    }

    public static Set<String> chunks(ManifestOuterClass.Manifest manifest, ModelAssetSelector selector) {
        if (selector instanceof ModelAssetSelector.ModelPreview) {
            return Set.of(ModelFileConstant.THUMB_BUTTON_CHUNK_NAME);
        }
        if (selector instanceof ModelAssetSelector.RenderTarget target) {
            return renderTargetChunks(manifest, target);
        }
        if (selector instanceof ModelAssetSelector.ModelPresentation presentation) {
            return presentationChunks(manifest, presentation);
        }
        if (selector instanceof ModelAssetSelector.PackCover) {
            return Set.of();
        }
        throw new IllegalArgumentException("Unsupported asset selector: " + selector);
    }

    public static boolean isCached(ModelDescriptor descriptor, ModelAssetSelector selector,
                                   Predicate<AssetContainerView.ChunkInfo> chunkExists) {
        var view = descriptor.view();
        for (var name : chunks(view.getManifest(), selector)) {
            var chunk = view.getFileView().getAssetView().getChunkInfo(name);
            if (chunk == null || chunk.hash() == null || !chunkExists.test(chunk)) {
                return false;
            }
        }
        return true;
    }

    public static String chooseTexture(ManifestOuterClass.Manifest manifest, String targetId, String requested) {
        var target = target(manifest, targetId);
        if (requested != null && !requested.isBlank()) {
            texture(target, requested);
            return requested;
        }
        var settings = manifest.getInfo().getSettings();
        if (settings.hasDefaultTexture() && containsTexture(target, settings.getDefaultTexture())) {
            return settings.getDefaultTexture();
        }
        for (var entry : target.getTextures()) {
            return entry.getKey();
        }
        throw new IllegalArgumentException("Render target has no texture: " + targetId);
    }

    public static RenderTargetOuterClass.RenderTarget target(ManifestOuterClass.Manifest manifest, String targetId) {
        for (var target : manifest.getRenderTargets()) {
            if (target.getTargetId().equals(targetId)) {
                return target;
            }
        }
        throw new IllegalArgumentException("Unknown render target: " + targetId);
    }

    private static Set<String> renderTargetChunks(ManifestOuterClass.Manifest manifest,
                                                   ModelAssetSelector.RenderTarget selector) {
        var target = target(manifest, selector.renderTargetId());
        var chunks = new HashSet<String>();
        for (var component : selector.components()) {
            switch (component) {
                case DEFINITION -> addBlob(chunks, target.getBlobId());
                case TEXTURE_SET -> addTexture(chunks,
                        texture(target, selector.textureName()));
                case COMMON_BEHAVIOR -> {
                    if (manifest.hasCommonBehavior() && manifest.getCommonBehavior().hasStringsBlobId()) {
                        addBlob(chunks, manifest.getCommonBehavior().getStringsBlobId());
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported render target component: " + component);
            }
        }
        return Set.copyOf(chunks);
    }

    private static Set<String> presentationChunks(ManifestOuterClass.Manifest manifest,
                                                   ModelAssetSelector.ModelPresentation selector) {
        var chunks = new HashSet<String>();
        var info = manifest.getInfo();
        switch (selector.asset()) {
            case MODEL_ICON -> chunks.add(ModelFileConstant.THUMB_ICON_CHUNK_NAME);
            case AUTHOR_AVATAR -> {
                if (!info.hasMetadata() || !info.getMetadata().hasAuthors()
                        || selector.index() >= info.getMetadata().getAuthors().length()) {
                    throw new IllegalArgumentException("Unknown author avatar index: " + selector.index());
                }
                var author = info.getMetadata().getAuthors().get(selector.index());
                if (!author.hasAvatar()) {
                    throw new IllegalArgumentException("Author contains no avatar: " + selector.index());
                }
                addImage(chunks, author.getAvatar());
            }
            case GUI_FOREGROUND -> {
                if (!info.getSettings().hasGuiForeground()) {
                    throw new IllegalArgumentException("Model contains no GUI foreground");
                }
                addImage(chunks, info.getSettings().getGuiForeground());
            }
            case GUI_BACKGROUND -> {
                if (!info.getSettings().hasGuiBackground()) {
                    throw new IllegalArgumentException("Model contains no GUI background");
                }
                addImage(chunks, info.getSettings().getGuiBackground());
            }
        }
        return Set.copyOf(chunks);
    }

    private static boolean containsTexture(RenderTargetOuterClass.RenderTarget target, String name) {
        for (var entry : target.getTextures()) {
            if (entry.getKey().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static Texture.PBRTextureSet texture(RenderTargetOuterClass.RenderTarget target, String name) {
        for (var entry : target.getTextures()) {
            if (entry.getKey().equals(name)) {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException("Unknown texture " + name + " for render target "
                + target.getTargetId());
    }

    private static void addTexture(Set<String> chunks, Texture.PBRTextureSet texture) {
        addImage(chunks, texture.getUv());
        if (texture.hasNormal()) {
            addImage(chunks, texture.getNormal());
        }
        if (texture.hasSpecular()) {
            addImage(chunks, texture.getSpecular());
        }
    }

    private static void addImage(Set<String> chunks, ImageOuterClass.Image image) {
        addBlob(chunks, image.getBlobId());
    }

    private static void addBlob(Set<String> chunks, int blobId) {
        if (blobId > 0) {
            chunks.add(AssetFileConstant.BLOB_CHUNK_PREFIX + blobId);
        }
    }
}
