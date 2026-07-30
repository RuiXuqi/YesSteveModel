package com.elfmcys.ysm.network.message.model;

import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.proto.network.model.ModelAssetsProto;
import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Immutable transfer metadata with reference-counted ownership of its wire content. */
public final class PreparedTransfer implements AutoCloseable {
    private final int decodedSize;
    private final UniBuffer content;
    private final AssetTransferV0.TransferPayloadEncoding encoding;
    private final ModelAssetsProto.ModelAssetManifest manifest;
    private final AtomicBoolean closed = new AtomicBoolean();

    private PreparedTransfer(int decodedSize, UniBuffer content,
                             AssetTransferV0.TransferPayloadEncoding encoding,
                             ModelAssetsProto.ModelAssetManifest manifest) {
        if (decodedSize < 0) {
            throw new IllegalArgumentException("Negative decoded transfer size");
        }
        this.decodedSize = decodedSize;
        this.content = Objects.requireNonNull(content, "content");
        this.encoding = Objects.requireNonNull(encoding, "encoding");
        this.manifest = manifest;
    }

    public static PreparedTransfer catalog(int decodedSize, UniBuffer content, boolean zstd) {
        return new PreparedTransfer(decodedSize, content,
                zstd ? AssetTransferV0.TransferPayloadEncoding.TRANSFER_PAYLOAD_ENCODING_ZSTD
                        : AssetTransferV0.TransferPayloadEncoding.TRANSFER_PAYLOAD_ENCODING_PROTOBUF,
                null);
    }

    public static PreparedTransfer modelAssets(int decodedSize, UniBuffer attachments,
                                                ModelAssetsProto.ModelAssetManifest manifest) {
        return new PreparedTransfer(decodedSize, attachments,
                AssetTransferV0.TransferPayloadEncoding.TRANSFER_PAYLOAD_ENCODING_RAW_ATTACHMENTS,
                Objects.requireNonNull(manifest, "manifest"));
    }

    public int decodedSize() {
        return decodedSize;
    }

    public int contentSize() {
        return content.size();
    }

    public AssetTransferV0.TransferPayloadEncoding encoding() {
        return encoding;
    }

    public ModelAssetsProto.ModelAssetManifest manifest() {
        return manifest;
    }

    public UniBuffer acquireContent() {
        return content.acquire();
    }

    public PreparedTransfer acquire() {
        return new PreparedTransfer(decodedSize, content.acquire(), encoding, manifest);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            content.close();
        }
    }
}
