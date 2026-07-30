package com.elfmcys.ysm.network.message.model;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.buffer.BufferType;
import com.elfmcys.ysm.buffer.NativeBuffer;
import com.elfmcys.ysm.natives.Zstd;

public final class ZstdPayloadCompression {
    public static final int MAX_COMPRESSION_LEVEL = 22;
    public static final int DYNAMIC_COMPRESSION_LEVEL = 10;
    public static final int MAX_DECOMPRESSED_PAYLOAD = 128 * 1024 * 1024;

    private ZstdPayloadCompression() {
    }

    public static PreparedTransfer prepareDynamic(com.elfmcys.ysm.buffer.ArrayBuffer serialized) {
        var compressed = Zstd.compressAndHash(
                serialized, null, BufferType.NATIVE, DYNAMIC_COMPRESSION_LEVEL);
        if (compressed.size() >= serialized.size()) {
            compressed.close();
            return PreparedTransfer.catalog(serialized.size(), serialized.acquire(), false);
        }
        return PreparedTransfer.catalog(serialized.size(), compressed, true);
    }

    public static ArrayBuffer decompress(NativeBuffer compressed, int uncompressedSize) {
        if (uncompressedSize < 0 || uncompressedSize > MAX_DECOMPRESSED_PAYLOAD) {
            throw new IllegalArgumentException("Compressed model payload is too large");
        }
        var decompressed = Zstd.decompressAndValidate(
                compressed, uncompressedSize, null, BufferType.ARRAY);
        if (!(decompressed instanceof ArrayBuffer arrayBuffer)) {
            decompressed.close();
            throw new IllegalStateException("QuickBuffers payload decompression returned a non-array buffer");
        }
        return arrayBuffer;
    }
}
