package com.elfmcys.ysm.network.message.model;

import com.elfmcys.ysm.buffer.BufferType;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.natives.Zstd;

/** Snapshot-only ModelList preparation. This deliberately bypasses adaptive packet compression. */
public final class ModelListSnapshotEncoder {
    private ModelListSnapshotEncoder() {
    }

    public static PreparedTransfer encode(UniBuffer serialized) {
        var compressed = Zstd.compressAndHash(serialized, null, BufferType.NATIVE,
                ZstdPayloadCompression.MAX_COMPRESSION_LEVEL);
        return PreparedTransfer.catalog(serialized.size(), compressed, true);
    }
}
