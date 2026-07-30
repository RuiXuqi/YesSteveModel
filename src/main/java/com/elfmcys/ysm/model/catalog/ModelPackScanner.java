package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.model.domain.ModelPackDescriptor;
import com.elfmcys.ysm.model.domain.ModelPath;
import com.elfmcys.ysm.model.storage.ModelHashing;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Reads one model-pack manifest and its optional cover metadata. */
final class ModelPackScanner {
    private static final int MAX_PACK_ICON_SIZE = 1024 * 1024;

    private final Gson gson = new Gson();

    Optional<ModelPackDescriptor> scan(ModelCatalogSource root,
                                       Path directory) throws IOException {
        var manifest = directory.resolve("ysm-pack.json");
        if (!Files.isRegularFile(manifest)) {
            return Optional.empty();
        }
        final PackManifest source;
        try (var reader = Files.newBufferedReader(manifest)) {
            source = gson.fromJson(reader, PackManifest.class);
        }
        if (source == null) {
            throw new IOException("Empty model pack manifest");
        }

        var hierarchy = directory.equals(root.path())
                ? "" : ModelPath.relativeTo(root.path(), directory).value() + "/";
        var translations = new LinkedHashMap<String, ModelPackDescriptor.LocalizedText>();
        if (source.lang != null) {
            source.lang.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                var value = entry.getValue();
                translations.put(entry.getKey(), new ModelPackDescriptor.LocalizedText(
                        value == null ? "" : value.name,
                        value == null ? "" : value.description));
            });
        }

        var iconPath = directory.resolve("ysm-pack.png");
        var coverHash = new byte[0];
        var coverSize = 0;
        if (Files.isRegularFile(iconPath)) {
            var size = Files.size(iconPath);
            if (size > MAX_PACK_ICON_SIZE) {
                throw new IOException("Model pack icon is larger than " + MAX_PACK_ICON_SIZE + " bytes");
            }
            coverHash = ModelHashing.blake3(iconPath).bytes();
            coverSize = Math.toIntExact(size);
        }
        return Optional.of(new ModelPackDescriptor(root.rootKind(), hierarchy, source.name, source.description,
                translations, coverHash, coverSize == 0 ? "" : "png", coverSize));
    }

    private static final class PackManifest {
        private String name = "";
        private String description = "";
        private Map<String, PackTranslation> lang = Map.of();
    }

    private static final class PackTranslation {
        private String name = "";
        private String description = "";
    }
}
