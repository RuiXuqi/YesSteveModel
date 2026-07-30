package com.elfmcys.ysm.model.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BuiltinModelCatalogTest {
    @TempDir
    Path temp;

    @Test
    void replacesLegacyAndExampleDirectoriesFromBuiltinResources() throws Exception {
        var config = Files.createDirectories(temp.resolve("config"));
        var legacy = Files.createDirectories(config.resolve("builtin"));
        Files.writeString(legacy.resolve("legacy.txt"), "legacy");
        var example = Files.createDirectories(config.resolve("example/default"));
        Files.writeString(example.resolve("stale.txt"), "stale");

        var archive = temp.resolve("mod.jar");
        try (var fileSystem = FileSystems.newFileSystem(
                URI.create("jar:" + archive.toUri()), Map.of("create", "true"))) {
            var builtinResources = Files.createDirectories(fileSystem.getPath("/assets/mod/builtin/default"));
            Files.writeString(builtinResources.resolve("ysm.json"), "fresh");
            BuiltinModelCatalog.refreshExamples(fileSystem.getPath("/assets/mod/builtin"), config);
        }

        assertFalse(Files.exists(config.resolve("builtin")));
        assertFalse(Files.exists(config.resolve("example/default/stale.txt")));
        assertEquals("fresh", Files.readString(config.resolve("example/default/ysm.json")));
    }
}
