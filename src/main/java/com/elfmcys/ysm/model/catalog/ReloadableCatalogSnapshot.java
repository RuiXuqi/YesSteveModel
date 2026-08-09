package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.domain.ModelPackDescriptor;
import com.elfmcys.ysm.model.domain.ModelScanReport;
import com.elfmcys.ysm.model.storage.ModelFileHandle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record ReloadableCatalogSnapshot(
        long reloadGeneration,
        boolean ready,
        Map<ModelSourceKey, ModelSourceState> sources,
        Map<ModelPackSourceKey, ModelPackSourceState> packSources,
        Map<Hash256, ModelFileHandle> models,
        List<ModelPackDescriptor> packs,
        ModelScanReport report) {
    public ReloadableCatalogSnapshot {
        if (reloadGeneration < 0) {
            throw new IllegalArgumentException("reloadGeneration must not be negative");
        }
        sources = sortedMap(sources);
        packSources = sortedMap(packSources);
        models = models.values().stream()
                .sorted((left, right) -> left.location().compareTo(right.location()))
                .collect(LinkedHashMap::new,
                        (result, handle) -> result.put(handle.descriptor().modelHash(), handle),
                        LinkedHashMap::putAll);
        models = Collections.unmodifiableMap(models);
        packs = packs.stream().sorted().toList();
        Objects.requireNonNull(report, "report");
    }

    public static ReloadableCatalogSnapshot unready() {
        return new ReloadableCatalogSnapshot(0, false, Map.of(), Map.of(), Map.of(),
                List.of(), ModelScanReport.empty());
    }

    public Optional<ModelSourceState.Ready> find(CatalogBackingKey backing) {
        return sources.values().stream()
                .filter(ModelSourceState.Ready.class::isInstance)
                .map(ModelSourceState.Ready.class::cast)
                .filter(value -> value.handle().location().equals(backing.location())
                        && value.handle().backingIdentity().equals(backing.identity()))
                .findFirst();
    }

    private static <K extends Comparable<K>, V> Map<K, V> sortedMap(Map<K, V> input) {
        var result = new LinkedHashMap<K, V>();
        input.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(result);
    }
}
