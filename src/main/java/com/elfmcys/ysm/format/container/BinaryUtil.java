package com.elfmcys.ysm.format.container;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BinaryUtil {
    public static void writeShortString(DataOutput output, String str) throws IOException {
        var bytes = str.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("Byte array exceeds maximum allowed size of " + Byte.MAX_VALUE);
        }
        output.write(bytes.length);
        output.write(bytes);
    }

    public static String readShortString(DataInput input) throws IOException {
        var size = input.readByte();
        if (size < 0) {
            throw new IOException("Invalid short string length: " + size);
        }
        var bytes = new byte[size];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    public static void writeFixedString(DataOutput output, String str, int size) throws IOException {
        var bytes = str.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length > size) {
            throw new IllegalArgumentException("String utf8 byte size exceeds maximum allowed size of " + size);
        }
        output.write(bytes);
        for (var i = bytes.length; i < size; i++) {
            output.write(0);
        }
    }

    public static String readFixedString(DataInput input, int size) throws IOException {
        var bytes = new byte[size];
        input.readFully(bytes);
        while (bytes[size - 1] == 0) {
            if (--size == 0) {
                return "";
            }
        }
        return new String(bytes, 0, size, StandardCharsets.UTF_8);
    }
}
