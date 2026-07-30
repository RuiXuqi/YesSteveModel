package com.elfmcys.ysm.format.legacy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LegacyYsmHeaderTest {
    @TempDir
    Path temp;

    @Test
    void recognizesBigEndianRawV1AndV2() throws Exception {
        assertEquals(LegacyYsmHeader.Version.V1_RAW,
                LegacyYsmHeader.probe(writeRaw("v1.ysm", 1)));
        assertEquals(LegacyYsmHeader.Version.V2_RAW,
                LegacyYsmHeader.probe(writeRaw("v2.ysm", 2)));
    }

    @Test
    void recognizesBomSummaryAndLittleEndianV3() throws Exception {
        var summary = "unstable fixture".getBytes(StandardCharsets.UTF_8);
        var bytes = ByteBuffer.allocate(7 + summary.length + 1 + Integer.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF,
                        'Y', 'S', 'G', 'P'})
                .put(summary)
                .put((byte) 0)
                .putInt(3)
                .array();
        var file = temp.resolve("v3.ysm");
        Files.write(file, bytes);

        assertEquals(LegacyYsmHeader.Version.V3_ENCRYPTED,
                LegacyYsmHeader.probe(file));
    }

    @Test
    void rejectsUnknownAndUnsupportedHeaders() throws Exception {
        var unknown = temp.resolve("unknown.ysm");
        Files.writeString(unknown, "not a ysm archive");
        assertThrows(java.io.IOException.class, () -> LegacyYsmHeader.probe(unknown));
        assertThrows(java.io.IOException.class,
                () -> LegacyYsmHeader.probe(writeRaw("v4.ysm", 4)));
    }

    private Path writeRaw(String name, int version) throws Exception {
        var file = temp.resolve(name);
        Files.write(file, ByteBuffer.allocate(8)
                .put(new byte[]{'Y', 'S', 'G', 'P'})
                .putInt(version)
                .array());
        return file;
    }
}
