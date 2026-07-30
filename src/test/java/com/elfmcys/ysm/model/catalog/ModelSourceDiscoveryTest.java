package com.elfmcys.ysm.model.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ModelSourceDiscoveryTest {
    @TempDir
    Path temp;

    @Test
    void discoversModernLegacyAndArchiveSourcesWithoutDescendingIntoModels() throws Exception {
        var modern = Files.createDirectories(temp.resolve("category/modern"));
        Files.writeString(modern.resolve("ysm.json"), "{}");
        Files.createDirectories(modern.resolve("models"));
        Files.writeString(modern.resolve("models/main.json"), "not another model");

        var legacy = Files.createDirectories(temp.resolve("legacy"));
        Files.writeString(legacy.resolve("main.json"), "{}");
        Files.writeString(temp.resolve("packed.ysm"), "archive");

        var result = ModelSourceDiscovery.discover(temp);

        assertEquals(List.of("category/modern", "legacy", "packed.ysm"),
                result.models().stream().map(this::relative).toList());
        assertFalse(result.packDirectories().stream()
                .map(this::relative).anyMatch(path -> path.startsWith("category/modern/")));
        assertEquals(List.of(), result.failures());
    }

    @Test
    void classifiesMxcAndHistoricalYsmHeadersWithoutGuessing() throws Exception {
        Files.writeString(temp.resolve("current.mxc"), "validated when opened");
        Files.write(temp.resolve("v1.ysm"), rawHeader(1));
        Files.write(temp.resolve("v2.ysm"), rawHeader(2));
        Files.write(temp.resolve("v3.ysm"), v3Header());
        Files.writeString(temp.resolve("unknown.ysm"), "unknown header");

        var state = assertInstanceOf(RootInventoryState.Complete.class,
                ModelSourceDiscovery.inventory(new ModelCatalogSource(
                        CatalogRootKind.CUSTOM, temp, false)));
        Map<String, ModelSourceKind> kinds = state.sources().values().stream()
                .collect(Collectors.toMap(
                        value -> value.key().relativePath().value(),
                        value -> value.key().sourceKind()));

        assertEquals(ModelSourceKind.DIRECT_CONTAINER, kinds.get("current.mxc"));
        assertEquals(ModelSourceKind.CURRENT_RAW_ARCHIVE, kinds.get("v1.ysm"));
        assertEquals(ModelSourceKind.CURRENT_RAW_ARCHIVE, kinds.get("v2.ysm"));
        assertEquals(ModelSourceKind.LEGACY_ARCHIVE, kinds.get("v3.ysm"));
        assertEquals(ModelSourceKind.UNSUPPORTED_YSM, kinds.get("unknown.ysm"));
    }

    private static byte[] rawHeader(int version) {
        return ByteBuffer.allocate(8).put(new byte[]{'Y', 'S', 'G', 'P'})
                .putInt(version).array();
    }

    private static byte[] v3Header() {
        return ByteBuffer.allocate(7 + 1 + Integer.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF,
                        'Y', 'S', 'G', 'P'})
                .put((byte) 0)
                .putInt(3)
                .array();
    }

    private String relative(Path path) {
        return temp.relativize(path).toString().replace('\\', '/');
    }
}
