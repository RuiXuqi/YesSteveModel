package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.format.schema.file.AssetFileConstant;
import com.elfmcys.ysm.format.schema.model.ModelFileConstant;
import com.elfmcys.ysm.format.schema.model.ModelFileView;
import mixel.common.ImageOuterClass;

import java.util.LinkedHashSet;
import java.util.Set;

/** Raw payloads that remain lazy after default geometry and animations are preprocessed. */
public final class DefaultLazyChunkPlan {
    private DefaultLazyChunkPlan() {
    }

    public static Set<String> collect(ModelFileView view) {
        var result = new LinkedHashSet<String>();
        addIfPresent(view, result, ModelFileConstant.THUMB_BUTTON_CHUNK_NAME);
        addIfPresent(view, result, ModelFileConstant.THUMB_ICON_CHUNK_NAME);
        var info = view.getManifest().getInfo();
        if (info.hasSettings()) {
            var settings = info.getSettings();
            if (settings.hasGuiForeground()) {
                add(result, settings.getGuiForeground());
            }
            if (settings.hasGuiBackground()) {
                add(result, settings.getGuiBackground());
            }
        }
        if (info.hasMetadata() && info.getMetadata().hasAuthors()) {
            for (var author : info.getMetadata().getAuthors()) {
                if (author.hasAvatar()) {
                    add(result, author.getAvatar());
                }
            }
        }
        for (var target : view.getRenderTargets()) {
            for (var textureName : target.getTextureNames()) {
                var texture = uncheckedTexture(target, textureName);
                add(result, texture.getUv());
                if (texture.hasNormal()) {
                    add(result, texture.getNormal());
                }
                if (texture.hasSpecular()) {
                    add(result, texture.getSpecular());
                }
            }
        }
        var assetView = view.getFileView().getAssetView();
        assetView.getChunkTable().keySet().stream()
                .filter(type -> type.startsWith(AssetFileConstant.STREAM_CHUNK_PREFIX))
                .forEach(result::add);
        return Set.copyOf(result);
    }

    private static mixel.manifest.asset.Texture.PBRTextureSet
    uncheckedTexture(com.elfmcys.ysm.format.schema.model.views.RenderTargetView target,
                     String name) {
        try {
            return target.textureDescriptor(name);
        } catch (java.io.FileNotFoundException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void add(Set<String> result, ImageOuterClass.Image image) {
        result.add(AssetFileConstant.BLOB_CHUNK_PREFIX + image.getBlobId());
    }

    private static void addIfPresent(ModelFileView view, Set<String> result,
                                     String type) {
        if (view.getFileView().getAssetView().getChunkInfo(type) != null) {
            result.add(type);
        }
    }
}
