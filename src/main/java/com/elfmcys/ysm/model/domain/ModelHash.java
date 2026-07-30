package com.elfmcys.ysm.model.domain;

import com.elfmcys.ysm.natives.Blake3;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;

/** Stable semantic identity of a model. */
public final class ModelHash implements Comparable<ModelHash> {
    public static final int SIZE = Blake3.HASH_SIZE;
    private static final HexFormat HEX = HexFormat.of();

    private final byte[] bytes;
    private final int hashCode;
    private String text;

    public ModelHash(byte[] bytes) {
        if (bytes.length != SIZE) {
            throw new IllegalArgumentException("Model hash must contain exactly " + SIZE + " bytes");
        }
        this.bytes = bytes.clone();
        this.hashCode = Arrays.hashCode(this.bytes);
    }

    public ModelHash(byte[] bytes, int offset, int length) {
        if (length != SIZE || offset < 0 || offset > bytes.length - length) {
            throw new IllegalArgumentException("Model hash must contain exactly " + SIZE + " bytes");
        }
        this.bytes = Arrays.copyOfRange(bytes, offset, offset + length);
        this.hashCode = Arrays.hashCode(this.bytes);
    }

    public static ModelHash parse(String value) {
        if (value.length() != SIZE * 2) {
            throw new IllegalArgumentException("Model hash must contain exactly " + (SIZE * 2) + " hexadecimal characters");
        }
        return new ModelHash(HEX.parseHex(value));
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
    public int compareTo(ModelHash other) {
        return Arrays.compareUnsigned(bytes, other.bytes);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ModelHash other && Arrays.equals(bytes, other.bytes);
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
