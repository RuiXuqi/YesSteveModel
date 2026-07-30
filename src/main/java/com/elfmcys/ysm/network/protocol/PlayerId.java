package com.elfmcys.ysm.network.protocol;

import java.util.Arrays;

public final class PlayerId {
    public static final int SIZE = 16;
    private final byte[] bytes;

    public PlayerId(byte[] bytes) {
        if (bytes.length != SIZE) {
            throw new IllegalArgumentException("Player id must be exactly 16 bytes");
        }
        this.bytes = bytes.clone();
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    @Override
    public boolean equals(Object value) {
        return value instanceof PlayerId other && Arrays.equals(bytes, other.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
