package com.elfmcys.ysm.format.schema.baked.model;

import com.elfmcys.ysm.buffer.BufferType;
import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.format.container.AssetContainerReader;
import com.elfmcys.ysm.format.container.AssetContainerView;
import com.elfmcys.ysm.format.container.InlineChunkReader;
import com.elfmcys.ysm.natives.Blake3;
import com.elfmcys.ysm.util.ProtoUtil;
import mixel.asset.model.data.GeoModelOuterClass;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;

public class BakedModelView {
    private final AssetContainerView assetView;
    private final GeoModelOuterClass.GeoModelIndex modelIndex;
    private final byte[] modelHash;

    public BakedModelView(SeekableByteChannel file) throws IOException {
        assetView = AssetContainerReader.read(file);
        if (!BakedModelConstant.SCHEMA_ID.equals(assetView.getSchema())) {
            throw new IOException("Schema ID mismatch: " + assetView.getSchema());
        }

        var version = new DefaultArtifactVersion(Objects.requireNonNull(
                assetView.getSchemaProperty(BakedModelConstant.PROP_VERSION),
                "Version property not found"));
        if (!BakedModelConstant.CURRENT_VERSION.equals(version)) {
            throw new UnsupportedEncodingException(String.format(
                    "Unsupported baked model version: \"%s\".", version));
        }
        if (!assetView.isAcceptedVersion()) {
            throw new UnsupportedEncodingException(String.format(
                    "Unsupported baked model container version: \"%s\".",
                    version));
        }

        var manifestChunk = assetView.getChunkInfo(BakedModelConstant.MANIFEST_CHUNK_NAME);
        if (manifestChunk == null) {
            throw new IOException("No model index found");
        }
        try (var manifestData = InlineChunkReader.readPayload(file, manifestChunk, BufferType.ARRAY)) {
            if (manifestData.size() == 0) {
                throw new IOException("No model index found");
            }
            if (!(manifestData instanceof ArrayBuffer arrayBuffer)) {
                throw new IOException("Baked model manifest is not array-backed");
            }
            modelIndex = GeoModelOuterClass.GeoModelIndex.parseFrom(ProtoUtil.source(arrayBuffer));
        }
        var hashChunk = assetView.getChunkInfo(BakedModelConstant.MODEL_HASH_CHUNK_NAME);
        if (hashChunk == null) {
            throw new IOException("Invalid model hash");
        }
        try (var hashData = InlineChunkReader.readPayload(file, hashChunk, BufferType.ARRAY)) {
            if (hashData.size() != Blake3.HASH_SIZE) {
                throw new IOException("Invalid model hash");
            }
            modelHash = new byte[Blake3.HASH_SIZE];
            if (!(hashData instanceof ArrayBuffer arrayBuffer)) {
                throw new IOException("Baked model hash is not array-backed");
            }
            System.arraycopy(arrayBuffer.array(), arrayBuffer.arrayOffset(), modelHash, 0, modelHash.length);
        }
    }

    public GeoModelOuterClass.GeoModelIndex modelIndex() {
        return modelIndex;
    }

    public byte[] modelHash() {
        return modelHash.clone();
    }

    @Nullable
    public UniBuffer readModelData(SeekableByteChannel file) throws IOException {
        var chunk = assetView.getChunkInfo(BakedModelConstant.MODEL_CHUNK_NAME);
        return chunk == null ? null : InlineChunkReader.readPayload(file, chunk, BufferType.NATIVE);
    }
}
