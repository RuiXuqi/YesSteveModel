package com.elfmcys.ysm.format.parser;

import com.elfmcys.ysm.buffer.NativeBuffer;
import com.elfmcys.ysm.format.vfs.Directory;
import com.elfmcys.ysm.format.vfs.VirtualFileSystem;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.natives.Blake3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelParserTest {
    @Test
    void hashOnlyScanReadsTheParseResourceSetWithoutBuildingAssets() {
        try (var vfs = new MemoryVfs()) {
            vfs.add("ysm.json", """
                    {
                      "metadata": {
                        "authors": [
                          {"name": "test", "avatar": "avatars/test.png"}
                        ]
                      },
                      "properties": {
                        "gui_foreground": "gui/foreground.png",
                        "gui_background": "gui/background.png",
                        "icon": "icon.bin",
                        "thumbnail": "thumbnail.bin"
                      },
                      "files": {
                        "player": {
                          "model": {
                            "main": "models/main.json",
                            "arm": "models/arm.json"
                          },
                          "animation": {
                            "main": "animations/main.json",
                            "unsupported": "animations/ignored.json"
                          },
                          "animation_controllers": ["controllers/player.json"],
                          "texture": [
                            {
                              "uv": "textures/player.png",
                              "normal": "textures/player_n.png",
                              "specular": "textures/player_s.png"
                            }
                          ]
                        },
                        "projectiles": [
                          {
                            "match": ["minecraft:arrow"],
                            "model": "projectile/model.json",
                            "animation": "projectile/animation.json",
                            "controller": "projectile/controller.json",
                            "texture": "projectile/texture.png"
                          },
                          {
                            "match": [],
                            "model": "ignored/model.json",
                            "texture": "ignored/texture.png"
                          }
                        ],
                        "vehicles": [
                          {
                            "match": ["minecraft:boat"],
                            "model": "vehicle/model.json",
                            "texture": "vehicle/texture.png"
                          }
                        ],
                        "sound_path": "sounds",
                        "function_path": "functions",
                        "language_path": "lang"
                      }
                    }
                    """);

            var expectedTypes = new LinkedHashMap<String, String>();
            expectedTypes.put("ysm.json", "manifest");
            expectedTypes.put("models/main.json", "model");
            expectedTypes.put("models/arm.json", "model");
            expectedTypes.put("animations/main.json", "animation");
            expectedTypes.put("controllers/player.json", "controller");
            expectedTypes.put("textures/player.png", "texture");
            expectedTypes.put("textures/player_n.png", "texture/normal");
            expectedTypes.put("textures/player_s.png", "texture/specular");
            expectedTypes.put("gui/foreground.png", "gui-foreground");
            expectedTypes.put("gui/background.png", "gui-background");
            expectedTypes.put("avatars/test.png", "avatar");
            expectedTypes.put("projectile/model.json", "model");
            expectedTypes.put("projectile/animation.json", "animation");
            expectedTypes.put("projectile/controller.json", "controller");
            expectedTypes.put("projectile/texture.png", "texture");
            expectedTypes.put("vehicle/model.json", "model");
            expectedTypes.put("vehicle/texture.png", "texture");
            expectedTypes.put("functions/main.molang", "molang-func");
            expectedTypes.put("functions/nested/extra.molang", "molang-func");
            expectedTypes.put("lang/en_us.json", "language");
            expectedTypes.put("lang/nested/zh_cn.json", "language");
            expectedTypes.put("icon.bin", "icon");
            expectedTypes.put("thumbnail.bin", "thumbnail");

            for (var path : expectedTypes.keySet()) {
                if (!path.equals("ysm.json")) {
                    vfs.add(path, "invalid asset: " + path);
                }
            }
            vfs.add("animations/ignored.json", "not read");
            vfs.add("ignored/model.json", "not read");
            vfs.add("ignored/texture.png", "not read");
            vfs.add("functions/ignored.txt", "not read");
            vfs.add("lang/ignored.lang", "not read");
            vfs.add("sounds/ignored.ogg", "not read");

            var actual = ModelParser.scanModelHash(vfs);

            assertEquals(expectedTypes.keySet(), vfs.readFiles());
            assertEquals(expectedHash(vfs, expectedTypes), actual);
        }
    }

    @Test
    void hashOnlyScanSupportsLegacyLayoutWithoutParsingAssets() {
        try (var vfs = new MemoryVfs()) {
            var expectedTypes = new LinkedHashMap<String, String>();
            expectedTypes.put("info.json", "info");
            expectedTypes.put("main.json", "model");
            expectedTypes.put("arm.json", "model");
            expectedTypes.put("main.animation.json", "animation");
            expectedTypes.put("skin.png", "texture");

            vfs.add("info.json", "{}");
            vfs.add("main.json", "invalid model");
            vfs.add("arm.json", "invalid model");
            vfs.add("main.animation.json", "invalid animation");
            vfs.add("skin.png", "invalid image");

            var actual = ModelParser.scanModelHash(vfs);

            assertEquals(expectedTypes.keySet(), vfs.readFiles());
            assertEquals(expectedHash(vfs, expectedTypes), actual);
        }
    }

    @Test
    void scanAndParseProduceTheSameHash(@TempDir Path outputDirectory) throws URISyntaxException {
        var manifest = Objects.requireNonNull(ModelParserTest.class.getResource(
                "/assets/ysm/builtin/wine_fox/22_elf/ysm.json"));
        var sourceDirectory = Path.of(manifest.toURI()).getParent();

        Hash256 scanned;
        try (var vfs = new Directory(sourceDirectory)) {
            scanned = ModelParser.scanModelHash(vfs);
        }

        Path output;
        try (var vfs = new Directory(sourceDirectory)) {
            output = ModelParser.parse(vfs, outputDirectory,
                    DefaultAnimationFilter.keepAll());
        }

        assertEquals(scanned + ".mxc", output.getFileName().toString());
    }

    private static Hash256 expectedHash(MemoryVfs vfs, Map<String, String> expectedTypes) {
        var canonicalizer = new ModelHashCanonicalizer();
        for (var entry : expectedTypes.entrySet()) {
            canonicalizer.add(entry.getValue(), entry.getKey(), Blake3.computeHash(vfs.file(entry.getKey())));
        }
        return new Hash256(canonicalizer.aggregate());
    }

    private static final class MemoryVfs implements VirtualFileSystem, AutoCloseable {
        private final Map<String, NativeBuffer> files = new LinkedHashMap<>();
        private final Set<String> readFiles = new LinkedHashSet<>();

        void add(String path, String content) {
            var data = content.getBytes(StandardCharsets.UTF_8);
            var buffer = NativeBuffer.allocate(data.length);
            buffer.nio().put(data);
            files.put(normalize(path), buffer);
        }

        NativeBuffer file(String path) {
            return files.get(normalize(path));
        }

        Set<String> readFiles() {
            return readFiles;
        }

        @Override
        public String[] listFiles(String path) {
            var prefix = directoryPrefix(path);
            return files.keySet().stream()
                    .filter(file -> file.startsWith(prefix))
                    .map(file -> file.substring(prefix.length()))
                    .filter(file -> !file.contains("/"))
                    .sorted()
                    .toArray(String[]::new);
        }

        @Override
        public String[] listDirectories(String path) {
            var prefix = directoryPrefix(path);
            return files.keySet().stream()
                    .filter(file -> file.startsWith(prefix))
                    .map(file -> file.substring(prefix.length()))
                    .filter(file -> file.contains("/"))
                    .map(file -> file.substring(0, file.indexOf('/')))
                    .distinct()
                    .sorted()
                    .toArray(String[]::new);
        }

        @Override
        public boolean hasFile(String fileName) {
            return files.containsKey(normalize(fileName));
        }

        @Override
        public NativeBuffer getFile(String fileName) {
            var normalized = normalize(fileName);
            var file = files.get(normalized);
            if (file != null) {
                readFiles.add(normalized);
            }
            return file;
        }

        @Override
        public void close() {
            files.values().forEach(NativeBuffer::close);
        }

        private static String directoryPrefix(String path) {
            var normalized = normalize(path);
            return normalized.isEmpty() ? "" : normalized + "/";
        }

        private static String normalize(String path) {
            if (path == null) {
                return "";
            }
            var normalized = path.replace('\\', '/');
            return Arrays.stream(normalized.split("/"))
                    .filter(part -> !part.isEmpty())
                    .reduce((left, right) -> left + "/" + right)
                    .orElse("");
        }
    }
}
