package com.elfmcys.ysm.format.schema.baked.asset;

import com.elfmcys.ysm.format.schema.file.AssetFileWriter;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.storage.ModelHashing;
import mixel.asset.model.data.AnimationOuterClass;
import com.elfmcys.ysm.proto.baked.asset.AssetManifest;
import com.elfmcys.ysm.util.ProtoBytes;
import com.elfmcys.ysm.util.ProtoUtil;

import java.io.IOException;
import java.util.HashSet;

/** Writes one immutable baked target asset with one zstd chunk per animation. */
public final class BakedAssetWriter extends AssetFileWriter {
    public BakedAssetWriter() {
        setSchemaId(BakedAssetConstant.SCHEMA_ID);
        setProperty(BakedAssetConstant.PROP_VERSION, BakedAssetConstant.CURRENT_VERSION.toString());
    }

    public void setData(Hash256 modelHash, Hash256 descriptorHash, String renderTargetId,
                        Hash256 definitionHash, Iterable<AnimationOuterClass.Animation> animations)
            throws IOException {
        var manifest = AssetManifest.BakedAssetManifest.newInstance()
                .setRenderTargetId(renderTargetId);
        ProtoBytes.set(manifest.getMutableModelHash(), modelHash);
        ProtoBytes.set(manifest.getMutableDescriptorHash(), descriptorHash);
        ProtoBytes.set(manifest.getMutableDefinitionHash(), definitionHash);
        var names = new HashSet<String>();
        var index = 0;
        for (var animation : animations) {
            if (!names.add(animation.getName())) {
                throw new IOException("Duplicate animation name in render target: " + animation.getName());
            }
            var chunkName = BakedAssetConstant.ANIM_CHUNK_PREFIX + "%06d".formatted(index++);
            var sourceHash = ModelHashing.blake3(ProtoUtil.serializeToArray(animation));
            var entry = AssetManifest.BakedAnimationEntry.newInstance()
                    .setName(animation.getName())
                    .setChunkName(chunkName);
            ProtoBytes.set(entry.getMutableSourceHash(), sourceHash);
            manifest.addAnimations(entry);
            addProtoChunk(chunkName, animation, 16);
        }
        addProtoChunk(BakedAssetConstant.MANIFEST_CHUNK_NAME, manifest, 0);
    }
}
