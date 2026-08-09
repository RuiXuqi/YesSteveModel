package com.elfmcys.ysm.client.model.catalog;

import com.elfmcys.ysm.model.catalog.RemoteCatalogSnapshot;
import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.domain.ModelPackDescriptor;
import com.elfmcys.ysm.model.source.CatalogCursor;
import com.elfmcys.ysm.model.source.ModelSource;
import com.elfmcys.ysm.model.source.ModelSourceAggregator;
import com.elfmcys.ysm.model.source.ModelSources;
import com.elfmcys.ysm.model.source.PackOffer;
import com.elfmcys.ysm.model.storage.ModelFileHandle;
import com.elfmcys.ysm.model.storage.ModelBackingIdentity;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongSupplier;

public record ClientCatalogSnapshot(long generation,
                                    Map<Hash256, ClientCatalogEntry> models,
                                    List<PackOffer> packs,
                                    ModelSourceAggregator.AggregatedCatalog sources,
                                    @Nullable RemoteCatalogSnapshot server) {
    public ClientCatalogSnapshot {
        models = Map.copyOf(models);
        packs = List.copyOf(packs);
    }

    public static ClientCatalogSnapshot empty() {
        var sources = ModelSourceAggregator.aggregate(List.of());
        return new ClientCatalogSnapshot(0, Map.of(), List.of(), sources, null);
    }

    public static ClientCatalogSnapshot merge(ClientCatalogSnapshot previous, long generation,
                                               Map<Hash256, ModelFileHandle> local,
                                               List<ModelPackDescriptor> localPacks,
                                               @Nullable RemoteCatalogSnapshot server,
                                               Set<ModelBackingIdentity> forcedVersions,
                                               LongSupplier nextContentVersion) {
        var entries = new LinkedHashMap<Hash256, ClientCatalogEntry>();
        var hashes = new java.util.HashSet<>(local.keySet());
        if (server != null) {
            hashes.addAll(server.models().keySet());
        }
        for (var hash : hashes.stream().sorted().toList()) {
            var handle = local.get(hash);
            var remote = server == null ? null : server.models().get(hash);
            var backing = handle != null ? handle.backingIdentity()
                    : new ModelBackingIdentity.RemoteSession(server.epoch(), hash,
                    Objects.requireNonNull(remote).descriptor().descriptorHash());
            var old = previous.models().get(hash);
            var sameVersion = old != null
                    && old.backingIdentity().equals(backing)
                    && old.displayDescriptor().sameRepresentation(
                    handle != null ? handle.descriptor() : Objects.requireNonNull(remote).descriptor())
                    && !forcedVersions.contains(backing);
            var version = sameVersion ? old.contentVersion()
                    : new ModelContentVersion(nextContentVersion.getAsLong());
            entries.put(hash, new ClientCatalogEntry(hash, version, backing, handle, remote));
        }
        var sources = new java.util.ArrayList<ModelSource>();
        for (var rootKind : CatalogRootKind.values()) {
            sources.add(new LocalModelSource(rootKind, local.values(), localPacks));
        }
        if (server != null) {
            sources.add(new GameServerModelSource(server));
        }
        var aggregated = ModelSourceAggregator.aggregate(sources);
        return new ClientCatalogSnapshot(generation, entries, aggregated.packs(), aggregated, server);
    }

    public Optional<ClientCatalogEntry> find(Hash256 hash) {
        return Optional.ofNullable(models.get(hash));
    }

    public long serverRevision() {
        return server == null ? 0 : server.revision();
    }

    public Optional<CatalogCursor> serverCursor() {
        return Optional.ofNullable(sources.cursors().get(ModelSources.GAME_SERVER));
    }

}
