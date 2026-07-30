package com.elfmcys.ysm.format.legacy;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/** Strictly identifies the historical formats that used the {@code .ysm} suffix. */
public final class LegacyYsmHeader {
    private static final byte[] RAW_MAGIC = {'Y', 'S', 'G', 'P'};
    private static final byte[] V3_MAGIC = {
            (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'Y', 'S', 'G', 'P'
    };
    private static final int MAX_V3_SUMMARY_BYTES = 1024 * 1024;

    private LegacyYsmHeader() {
    }

    public static Version probe(Path source) throws IOException {
        try (var input = new BufferedInputStream(Files.newInputStream(source))) {
            var prefix = input.readNBytes(8);
            if (prefix.length != 8) {
                throw new IOException("Truncated .ysm header");
            }
            if (startsWith(prefix, RAW_MAGIC)) {
                var version = ByteBuffer.wrap(prefix, RAW_MAGIC.length, Integer.BYTES)
                        .getInt();
                return switch (version) {
                    case 1 -> Version.V1_RAW;
                    case 2 -> Version.V2_RAW;
                    default -> throw new IOException(
                            "Unsupported raw .ysm version: " + Integer.toUnsignedString(version));
                };
            }
            if (!startsWith(prefix, V3_MAGIC)) {
                throw new IOException("Unknown .ysm header");
            }

            var summaryBytes = 0;
            var value = Byte.toUnsignedInt(prefix[V3_MAGIC.length]);
            while (value != 0) {
                if (++summaryBytes > MAX_V3_SUMMARY_BYTES) {
                    throw new IOException("V3 .ysm summary is too large");
                }
                value = input.read();
                if (value < 0) {
                    throw new IOException("Truncated V3 .ysm summary");
                }
            }
            var versionBytes = input.readNBytes(Integer.BYTES);
            if (versionBytes.length != Integer.BYTES) {
                throw new IOException("Truncated V3 .ysm version");
            }
            var version = ByteBuffer.wrap(versionBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (version != 3) {
                throw new IOException(
                        "Unsupported encrypted .ysm version: " + Integer.toUnsignedString(version));
            }
            return Version.V3_ENCRYPTED;
        }
    }

    private static boolean startsWith(byte[] input, byte[] expected) {
        if (input.length < expected.length) {
            return false;
        }
        for (var i = 0; i < expected.length; i++) {
            if (input[i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    public enum Version {
        V1_RAW,
        V2_RAW,
        V3_ENCRYPTED
    }
}
