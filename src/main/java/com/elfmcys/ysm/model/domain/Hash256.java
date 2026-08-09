package com.elfmcys.ysm.model.domain;

import com.elfmcys.ysm.natives.Blake3;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;

/** Stable semantic identity of a model. */
public final class Hash256 implements Comparable<Hash256> {
    public static final int SIZE = Blake3.HASH_SIZE;
    private static final HexFormat HEX = HexFormat.of();

    private final byte[] bytes;
    private final int hashCode;
    private String text;

    public Hash256(byte[] bytes) {
        if (bytes.length != SIZE) {
            throw new IllegalArgumentException("Model hash must contain exactly " + SIZE + " bytes");
        }
        this.bytes = bytes.clone();
        this.hashCode = Arrays.hashCode(this.bytes);
    }

    public Hash256(byte[] bytes, int offset, int length) {
        if (length != SIZE || offset < 0 || offset > bytes.length - length) {
            throw new IllegalArgumentException("Model hash must contain exactly " + SIZE + " bytes");
        }
        this.bytes = Arrays.copyOfRange(bytes, offset, offset + length);
        this.hashCode = Arrays.hashCode(this.bytes);
    }

    public static Hash256 parse(String value) {
        if (value.length() != SIZE * 2) {
            throw new IllegalArgumentException("Model hash must contain exactly " + (SIZE * 2) + " hexadecimal characters");
        }
        return new Hash256(HEX.parseHex(value));
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    public boolean matches(byte[] value, int offset, int length) {
        return length == SIZE && offset >= 0 && offset <= value.length - length
                && Arrays.equals(bytes, 0, SIZE, value, offset, offset + length);
    }

    public boolean matches(byte[] value) {
        return matches(value, 0, value.length);
    }

    /** Only roaming-variable synchronization is allowed to use this truncated value. */
    public int roamingHash() {
        return ByteBuffer.wrap(bytes, 0, Integer.BYTES).getInt();
    }

    @Override
    public int compareTo(Hash256 other) {
        return Arrays.compareUnsigned(bytes, other.bytes);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Hash256 other && Arrays.equals(bytes, other.bytes);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        if (text == null) {
            text = HEX.formatHex(bytes);
        }
        return text;
    }
}
