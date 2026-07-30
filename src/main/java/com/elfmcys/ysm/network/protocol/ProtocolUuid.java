package com.elfmcys.ysm.network.protocol;

import us.hebi.quickbuf.RepeatedByte;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class ProtocolUuid {
    public static final int SIZE = 16;

    private ProtocolUuid() {
    }

    public static void set(RepeatedByte target, UUID value) {
        var bytes = new byte[SIZE];
        ByteBuffer.wrap(bytes)
                .putLong(value.getMostSignificantBits())
                .putLong(value.getLeastSignificantBits());
        target.setInternalArray(bytes);
    }

    public static UUID get(RepeatedByte source) {
        if (source.length() != SIZE) {
            throw new IllegalArgumentException("Protocol UUID must contain exactly 16 bytes");
        }
        var buffer = ByteBuffer.wrap(source.array(), 0, source.length());
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
