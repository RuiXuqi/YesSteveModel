package com.elfmcys.ysm.model.source;

import com.elfmcys.ysm.model.domain.ModelHash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Deterministically merges immutable source catalogs without owning asset transport. */
public final class ModelSourceAggregator {
    private ModelSourceAggregator() {
    }

    public static AggregatedCatalog aggregate(Collection<? extends ModelSource> sources) {
        var catalogs = new ArrayList<ResolvedCatalog>(sources.size());
        var priorities = new LinkedHashMap<SourceId, Integer>();
        for (var source : sources) {
            var descriptor = source.descriptor();
            var catalog = source.catalog();
            if (!descriptor.id().equals(catalog.cursor().sourceId())) {
                throw new IllegalArgumentException("Model source catalog belongs to a different source: "
                        + descriptor.id());
            }
            if (priorities.putIfAbsent(descriptor.id(), descriptor.priority()) != null) {
                throw new IllegalArgumentException("Duplicate model source id: " + descriptor.id());
            }
            catalogs.add(catalog);
        }
        var offerOrder = Comparator
                .<ModelOffer>comparingInt(offer -> -priorities.getOrDefault(offer.sourceId(), 0))
                .thenComparing(ModelOffer::sourceId)
                .thenComparing(ModelOffer::namespace)
                .thenComparing(ModelOffer::path);
        var offers = new LinkedHashMap<ModelHash, ArrayList<ModelOffer>>();
        var packs = new ArrayList<PackOffer>();
        var cursors = new LinkedHashMap<SourceId, CatalogCursor>();
        for (var catalog : catalogs) {
            if (cursors.putIfAbsent(catalog.cursor().sourceId(), catalog.cursor()) != null) {
                throw new IllegalArgumentException("Duplicate resolved catalog source: "
                        + catalog.cursor().sourceId());
            }
            catalog.models().forEach(offer -> offers
                    .computeIfAbsent(offer.descriptor().modelHash(), ignored -> new ArrayList<>()).add(offer));
            packs.addAll(catalog.packs());
        }
        var immutableOffers = new LinkedHashMap<ModelHash, List<ModelOffer>>();
        offers.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            entry.getValue().sort(offerOrder);
            immutableOffers.put(entry.getKey(), List.copyOf(entry.getValue()));
        });
        packs.sort(Comparator.<PackOffer>comparingInt(pack -> -priorities.getOrDefault(pack.sourceId(), 0))
                .thenComparing(PackOffer::sourceId)
                .thenComparing(pack -> pack.subject().namespace())
                .thenComparing(pack -> pack.subject().hierarchy()));
        return new AggregatedCatalog(Map.copyOf(immutableOffers), List.copyOf(packs), Map.copyOf(cursors));
    }

    public record AggregatedCatalog(Map<ModelHash, List<ModelOffer>> offers,
                                    List<PackOffer> packs,
                                    Map<SourceId, CatalogCursor> cursors) {
        public AggregatedCatalog {
            offers = Map.copyOf(offers);
            packs = List.copyOf(packs);
            cursors = Map.copyOf(cursors);
        }

        public Optional<ModelOffer> preferred(ModelHash hash) {
            var matches = offers.get(hash);
            return matches == null || matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
        }

        public Optional<ModelHash> resolvePath(String path) {
            ModelHash found = null;
            for (var entry : offers.entrySet()) {
                if (entry.getValue().stream().anyMatch(offer -> offer.path().value().equals(path))) {
                    if (found != null && !found.equals(entry.getKey())) {
                        return Optional.empty();
                    }
                    found = entry.getKey();
                }
            }
            return Optional.ofNullable(found);
        }

        public Optional<ModelOffer> resolve(SourceId sourceId, String namespace, String path) {
            return offers.values().stream().flatMap(List::stream)
                    .filter(offer -> offer.sourceId().equals(sourceId)
                            && offer.namespace().equals(namespace)
                            && offer.path().value().equals(path))
                    .findFirst();
        }
    }
}
