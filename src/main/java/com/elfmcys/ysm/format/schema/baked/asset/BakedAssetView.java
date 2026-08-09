package com.elfmcys.ysm.format.schema.baked.asset;

import com.elfmcys.ysm.buffer.BufferType;
import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.client.model.AnimationStore;
import com.elfmcys.ysm.client.model.ModelResourceFailureGate;
import com.elfmcys.ysm.format.container.AssetContainerReader;
import com.elfmcys.ysm.format.container.AssetContainerView;
import com.elfmcys.ysm.format.container.InlineChunkReader;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.storage.ModelHashing;
import mixel.asset.model.data.AnimationOuterClass;
import com.elfmcys.ysm.proto.baked.asset.AssetManifest;
import com.elfmcys.ysm.util.ProtoBytes;
import com.elfmcys.ysm.util.ProtoUtil;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.Nullable;
import us.hebi.quickbuf.RepeatedByte;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.SeekableByteChannel;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Metadata view for a baked render target. Animation payloads remain unopened. */
public final class BakedAssetView {
    private final AssetContainerView assetView;
    private final AssetManifest.BakedAssetManifest manifest;
    private final Map<String, AssetManifest.BakedAnimationEntry> animations;

    public BakedAssetView(SeekableByteChannel file) throws IOException {
        assetView = AssetContainerReader.read(file);
        if (!BakedAssetConstant.SCHEMA_ID.equals(assetView.getSchema())) {
            throw new IOException("Schema ID mismatch: " + assetView.getSchema());
        }
        var version = new DefaultArtifactVersion(Objects.requireNonNull(assetView.getSchemaProperty(
                BakedAssetConstant.PROP_VERSION), "Version property not found"));
        if (!version.equals(BakedAssetConstant.CURRENT_VERSION)
                || !assetView.isAcceptedVersion()) {
            throw new UnsupportedEncodingException("Unsupported baked asset version: \"" + version + "\"");
        }
        var manifestChunk = assetView.getChunkInfo(BakedAssetConstant.MANIFEST_CHUNK_NAME);
        if (manifestChunk == null) {
            throw new IOException("Baked asset contains no manifest");
        }
        try (var data = InlineChunkReader.readPayload(file, manifestChunk, BufferType.ARRAY)) {
            if (data.size() == 0) {
                throw new IOException("Baked asset contains no manifest");
            }
            if (!(data instanceof ArrayBuffer arrayBuffer)) {
                throw new IOException("Baked asset manifest is not array-backed");
            }
            manifest = AssetManifest.BakedAssetManifest.parseFrom(ProtoUtil.source(arrayBuffer));
        }
        validateHash(manifest.getModelHash(), "model hash");
        validateHash(manifest.getDescriptorHash(), "descriptor hash");
        validateHash(manifest.getDefinitionHash(), "definition hash");
        if (manifest.getRenderTargetId().isBlank()) {
            throw new IOException("Baked asset contains no render target id");
        }
        var index = new LinkedHashMap<String, AssetManifest.BakedAnimationEntry>();
        for (var entry : manifest.getAnimations()) {
            validateHash(entry.getSourceHash(), "animation source hash");
            if (entry.getName().isBlank() || entry.getChunkName().isBlank()
                    || assetView.getChunkInfo(entry.getChunkName()) == null
                    || index.putIfAbsent(entry.getName(), entry) != null) {
                throw new IOException("Invalid baked animation index entry: " + entry.getName());
            }
        }
        animations = Map.copyOf(index);
    }

    public boolean matches(Hash256 modelHash, Hash256 descriptorHash, String targetId,
                           Hash256 definitionHash) {
        return ProtoBytes.equals(modelHash, manifest.getModelHash())
                && ProtoBytes.equals(descriptorHash, manifest.getDescriptorHash())
                && targetId.equals(manifest.getRenderTargetId())
                && ProtoBytes.equals(definitionHash, manifest.getDefinitionHash());
    }

    @Nullable
    public AnimationOuterClass.Animation readAnimation(SeekableByteChannel file, String animationName)
            throws IOException {
        var entry = animations.get(animationName);
        if (entry == null) {
            return null;
        }
        var chunk = assetView.getChunkInfo(entry.getChunkName());
        if (chunk == null) {
            throw new IOException("Baked animation chunk is missing: " + entry.getChunkName());
        }
        try (var data = InlineChunkReader.readPayload(file, chunk, BufferType.ARRAY)) {
            if (data.size() == 0) {
                throw new IOException("Baked animation chunk is missing: " + entry.getChunkName());
            }
            if (!(data instanceof ArrayBuffer arrayBuffer)) {
                throw new IOException("Baked animation is not array-backed");
            }
            if (!ProtoBytes.equals(ModelHashing.blake3(arrayBuffer), entry.getSourceHash())) {
                throw new IOException("Baked animation source hash mismatch: " + animationName);
            }
            return AnimationOuterClass.Animation.parseFrom(ProtoUtil.source(arrayBuffer));
        }
    }

    public AnimationStore createAnimationStore(ChannelSource channels,
                                                AnimationStore.AnimationBinder binder) {
        return createAnimationStore(channels, binder, null);
    }

    public AnimationStore createAnimationStore(ChannelSource channels,
                                                AnimationStore.AnimationBinder binder,
                                                AnimationStore fallback) {
        return createAnimationStore(channels, binder, fallback,
                ignored -> ModelResourceFailureGate.none());
    }

    public AnimationStore createAnimationStore(ChannelSource channels,
                                                AnimationStore.AnimationBinder binder,
                                                AnimationStore fallback,
                                                Function<String, ModelResourceFailureGate> failureGates) {
        return AnimationStore.lazy(animations.keySet(), name -> {
            try (var channel = channels.open()) {
                return readAnimation(channel, name);
            }
        }, binder, fallback, failureGates);
    }

    private static void validateHash(RepeatedByte value, String name) throws IOException {
        if (value.length() != Hash256.SIZE) {
            throw new IOException("Invalid baked asset " + name);
        }
    }

    @FunctionalInterface
    public interface ChannelSource {
        SeekableByteChannel open() throws IOException;
    }
}
