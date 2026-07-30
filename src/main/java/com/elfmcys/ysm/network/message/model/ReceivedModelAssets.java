package com.elfmcys.ysm.network.message.model;

import com.elfmcys.ysm.buffer.NativeBuffer;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.proto.network.model.ModelAssetsProto;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Received model metadata plus encoded chunk/image attachments in their original bytes. */
public final class ReceivedModelAssets implements AutoCloseable {
    private final ModelAssetsProto.ModelAssetManifest manifest;
    private final NativeBuffer attachments;
    private final AtomicBoolean closed = new AtomicBoolean();

    public ReceivedModelAssets(ModelAssetsProto.ModelAssetManifest manifest, NativeBuffer attachments) {
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        this.attachments = Objects.requireNonNull(attachments, "attachments");
    }

    public ModelAssetsProto.ModelAssetManifest manifest() {
        return manifest;
    }

    public UniBuffer chunk(ModelAssetsProto.ModelChunk chunk) {
        var offset = chunk.getRawOffset();
        var size = chunk.getRawSize();
        if (offset < 0 || offset > Integer.MAX_VALUE || size < 0
                || offset > attachments.size() - size) {
            throw new IllegalArgumentException("Model asset chunk exceeds its raw attachment");
        }
        return attachments.slice((int) offset, size).acquire();
    }

    public int attachmentSize() {
        return attachments.size();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            attachments.close();
        }
    }
}
