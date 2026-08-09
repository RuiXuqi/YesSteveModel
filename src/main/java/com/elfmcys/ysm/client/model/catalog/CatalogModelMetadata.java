package com.elfmcys.ysm.client.model.catalog;

import com.elfmcys.ysm.client.lang.LanguageManager;
import com.elfmcys.ysm.client.model.internal.metadata.ManifestModelInfoMapper;
import com.elfmcys.ysm.info.ModelInfo;
import com.elfmcys.ysm.model.domain.ModelDescriptor;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.domain.RenderTargetIds;
import com.elfmcys.ysm.network.message.model.ModelAssetPlan;
import mixel.manifest.ManifestOuterClass;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Lightweight GUI metadata derived entirely from the catalog schema manifest. */
public record CatalogModelMetadata(Hash256 modelHash, String path, ModelDescriptor descriptor,
                                   ModelInfo info, Map<String, Map<String, String>> languages) {
    public CatalogModelMetadata {
        Objects.requireNonNull(modelHash, "modelHash");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(info, "info");
        languages = Map.copyOf(languages);
    }

    public static CatalogModelMetadata from(ClientCatalogEntry entry) {
        var descriptor = entry.displayDescriptor();
        ManifestOuterClass.Manifest manifest = descriptor.view().getManifest();
        var languages = new LinkedHashMap<String, Map<String, String>>();
        var info = manifest.getInfo();
        if (info.hasLanguageFiles()) {
            for (var language : info.getLanguageFiles()) {
                var entries = new LinkedHashMap<String, String>();
                if (language.hasEntries()) {
                    language.getEntries().forEach(value -> entries.put(value.getKey(), value.getValue()));
                }
                languages.put(language.getLocale(), Map.copyOf(entries));
            }
        }
        return new CatalogModelMetadata(entry.modelHash(), entry.displayPath(), descriptor,
                ManifestModelInfoMapper.map(entry.modelHash(), manifest), languages);
    }

    public String localized(String locale, String key, String fallback) {
        var selected = languages.get(locale);
        if (selected != null && selected.containsKey(key)) {
            return selected.get(key);
        }
        var defaults = languages.get(LanguageManager.DEFAULT_LANGUAGE_CODE);
        return defaults == null ? fallback : defaults.getOrDefault(key, fallback);
    }

    public String defaultTexture() {
        return ModelAssetPlan.chooseTexture(
                descriptor.view().getManifest(),
                RenderTargetIds.PLAYER, "");
    }
}
