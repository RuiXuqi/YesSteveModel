package com.elfmcys.ysm.network.message.model;

import com.elfmcys.ysm.format.schema.file.AssetFileConstant;
import com.elfmcys.ysm.model.source.ModelAssetSelector;
import mixel.common.ImageOuterClass;
import mixel.manifest.ManifestOuterClass;
import mixel.manifest.asset.RenderTargetOuterClass;
import mixel.manifest.asset.Texture;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelAssetPlanTest {
    @Test
    void playerPlanReturnsOneDefinitionAndOneSelectedTextureSet() {
        var texture = Texture.PBRTextureSet.newInstance()
                .setUv(ImageOuterClass.Image.newInstance().setBlobId(2))
                .setNormal(ImageOuterClass.Image.newInstance().setBlobId(3));
        var player = RenderTargetOuterClass.RenderTarget.newInstance()
                .setTargetId("player")
                .setKind(RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_PLAYER)
                .setBlobId(1)
                .addTextures(RenderTargetOuterClass.RenderTarget.TexturesEntry.newInstance()
                        .setKey("default").setValue(texture));
        var manifest = ManifestOuterClass.Manifest.newInstance().addRenderTargets(player);
        var selector = ModelAssetSelector.renderTarget("player", "default", EnumSet.of(
                ModelAssetSelector.RenderTargetComponent.DEFINITION,
                ModelAssetSelector.RenderTargetComponent.TEXTURE_SET));

        assertEquals(Set.of(
                        AssetFileConstant.BLOB_CHUNK_PREFIX + 1,
                        AssetFileConstant.BLOB_CHUNK_PREFIX + 2,
                        AssetFileConstant.BLOB_CHUNK_PREFIX + 3),
                ModelAssetPlan.chunks(manifest, selector));
    }
}
