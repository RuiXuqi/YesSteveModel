package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.domain.ModelPath;
import mixel.asset.model.data.AnimationOuterClass;
import mixel.manifest.asset.RenderTargetOuterClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinModelIndexTest {
    @Test
    void writesCanonicalSortedIndexAndReadsItBack() throws Exception {
        var index = BuiltinModelIndex.of(Map.of(
                new ModelPath("misc/example"), hash(2),
                new ModelPath("default"), hash(1)));
        var output = new StringWriter();

        index.write(output);

        var json = output.toString();
        assertTrue(json.indexOf("default") < json.indexOf("misc/example"));
        assertEquals(List.of(new ModelPath("default"), new ModelPath("misc/example")),
                BuiltinModelIndex.read(new StringReader(json)).entries().stream()
                        .map(BuiltinModelIndex.Entry::path).toList());
    }

    @Test
    void rejectsUnknownFieldsUnsupportedVersionsAndNonCanonicalValues() {
        assertInvalid("""
                {"formatVersion":1,"models":[{"path":"default","modelHash":"%s","extra":1}]}
                """.formatted(hash(1)));
        assertInvalid("""
                {"formatVersion":2,"models":[{"path":"default","modelHash":"%s"}]}
                """.formatted(hash(1)));
        assertInvalid("""
                {"formatVersion":1,"models":[{"path":"/default","modelHash":"%s"}]}
                """.formatted(hash(1)));
        assertInvalid("""
                {"formatVersion":1,"models":[{"path":"default","modelHash":"%s"}]}
                """.formatted(hash(0xab).toString().toUpperCase()));
    }

    @Test
    void rejectsUnsortedDuplicateHashMissingDefaultAndCoverageMismatch() throws Exception {
        assertInvalid("""
                {"formatVersion":1,"models":[
                  {"path":"misc/example","modelHash":"%s"},
                  {"path":"default","modelHash":"%s"}
                ]}
                """.formatted(hash(2), hash(1)));
        assertInvalid("""
                {"formatVersion":1,"models":[
                  {"path":"default","modelHash":"%s"},
                  {"path":"misc/example","modelHash":"%s"}
                ]}
                """.formatted(hash(1), hash(1)));
        assertInvalid("""
                {"formatVersion":1,"models":[{"path":"misc/example","modelHash":"%s"}]}
                """.formatted(hash(2)));

        var index = BuiltinModelIndex.of(Map.of(new ModelPath("default"), hash(1)));
        assertThrows(IOException.class,
                () -> index.validateCoverage(List.of(new ModelPath("different"))));
    }

    @Test
    void mergesKnownAnimationHistoryAndRejectsUnknownNames() throws Exception {
        var key = new DefaultAnimationKey("player/main", "idle");
        var current = hash(3);
        var historical = hash(4);
        var index = BuiltinModelIndex.of(
                Map.of(new ModelPath("default"), hash(1)),
                Map.of(key, current), Map.of(key, Set.of(historical)));

        assertTrue(index.accepts(key, current));
        assertTrue(index.accepts(key, historical));
        assertThrows(IOException.class, () -> BuiltinModelIndex.of(
                Map.of(new ModelPath("default"), hash(1)), Map.of(key, current),
                Map.of(new DefaultAnimationKey("player/main", "missing"), Set.of(historical))));
    }

    @Test
    void immutableContractFiltersAcceptedDefaultsAndValidatesCurrentPayloads() throws Exception {
        var target = RenderTargetOuterClass.RenderTarget.newInstance()
                .setKind(RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_PLAYER);
        var idle = AnimationOuterClass.Animation.newInstance().setName("idle").setLength(1);
        var custom = AnimationOuterClass.Animation.newInstance().setName("custom").setLength(2);
        var key = new DefaultAnimationKey("player/main", "idle");
        var index = BuiltinModelIndex.of(
                Map.of(new ModelPath("default"), hash(1)),
                Map.of(key, BuiltinModelMaterializer.payloadHash(idle)), Map.of());

        var filtered = index.apply(target, "main",
                AnimationOuterClass.AnimationFile.newInstance()
                        .addAnimations(idle).addAnimations(custom));

        assertEquals(1, filtered.getAnimations().length());
        assertEquals("custom", filtered.getAnimations().get(0).getName());
        assertDoesNotThrow(() -> index.requireCurrent(target, "main", idle));
        assertThrows(IOException.class, () -> index.requireCurrent(target, "main",
                AnimationOuterClass.Animation.newInstance().setName("idle").setLength(3)));
    }

    private static void assertInvalid(String json) {
        assertThrows(IOException.class, () -> BuiltinModelIndex.read(new StringReader(json)));
    }

    private static Hash256 hash(int marker) {
        var bytes = new byte[Hash256.SIZE];
        bytes[bytes.length - 1] = (byte) marker;
        return new Hash256(bytes);
    }
}
