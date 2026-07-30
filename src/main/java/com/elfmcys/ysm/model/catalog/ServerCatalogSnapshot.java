package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.domain.ModelPackDescriptor;
import com.elfmcys.ysm.model.domain.ModelScanReport;
import com.elfmcys.ysm.model.storage.ModelFileHandle;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ServerCatalogSnapshot {
    private final UUID epoch;
    private final long revision;
    private final Map<ModelHash, ModelFileHandle> byHash;
    private final Map<CatalogModelLocation, ModelHash> byLocation;
    private final List<ModelPackDescriptor> packs;
    private final ModelScanReport report;

    public ServerCatalogSnapshot(UUID epoch, long revision, Collection<ModelFileHandle> models,
                                 Collection<ModelPackDescriptor> packs, ModelScanReport report) {
        this.epoch = Objects.requireNonNull(epoch, "epoch");
        this.revision = revision;
        this.report = Objects.requireNonNull(report, "report");

        var hashIndex = new LinkedHashMap<ModelHash, ModelFileHandle>();
        var locationIndex = new LinkedHashMap<CatalogModelLocation, ModelHash>();
        models.stream().sorted((left, right) -> left.location().compareTo(right.location()))
                .forEach(model -> {
                    var descriptor = model.descriptor();
                    if (hashIndex.putIfAbsent(descriptor.modelHash(), model) != null) {
                        throw new IllegalArgumentException("Duplicate model hash in catalog: " + descriptor.modelHash());
                    }
                    if (locationIndex.putIfAbsent(model.location(), descriptor.modelHash()) != null) {
                        throw new IllegalArgumentException("Duplicate model path in catalog: " + model.location());
                    }
                });
        this.byHash = Map.copyOf(hashIndex);
        this.byLocation = Map.copyOf(locationIndex);
        this.packs = packs.stream().sorted().toList();
    }

    public static ServerCatalogSnapshot empty() {
        return new ServerCatalogSnapshot(UUID.randomUUID(), 0, List.of(), List.of(), ModelScanReport.empty());
    }

    public UUID epoch() {
        return epoch;
    }

    public byte[] epochBytes() {
        return ByteBuffer.allocate(16).putLong(epoch.getMostSignificantBits()).putLong(epoch.getLeastSignificantBits()).array();
    }

    public long revision() {
        return revision;
    }

    public Map<ModelHash, ModelFileHandle> models() {
        return byHash;
    }

    public List<ModelPackDescriptor> packs() {
        return packs;
    }

    public ModelScanReport report() {
        return report;
    }

    public Optional<ModelFileHandle> find(ModelHash hash) {
        return Optional.ofNullable(byHash.get(hash));
    }

    public Optional<ModelFileHandle> find(CatalogModelLocation location) {
        var hash = byLocation.get(location);
        return hash == null ? Optional.empty() : find(hash);
    }

    public Optional<ModelFileHandle> findPath(String path) {
        ModelFileHandle found = null;
        for (var entry : byLocation.entrySet()) {
            if (entry.getKey().path().value().equals(path)) {
                if (found != null) {
                    return Optional.empty();
                }
                found = byHash.get(entry.getValue());
            }
        }
        return Optional.ofNullable(found);
    }

    public Optional<ModelFileHandle> defaultModel() {
        return byHash.values().stream().filter(handle -> {
            var path = handle.location().path().value();
            return handle.location().rootKind() == CatalogRootKind.BUILTIN
                    && path.equals("default");
        }).findFirst();
    }
}
