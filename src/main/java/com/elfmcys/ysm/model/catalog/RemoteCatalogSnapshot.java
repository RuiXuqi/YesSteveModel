package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.source.ModelOffer;
import com.elfmcys.ysm.model.source.PackOffer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record RemoteCatalogSnapshot(UUID epoch, long revision, Map<Hash256, ModelOffer> models,
                                    List<PackOffer> packs) {
    public RemoteCatalogSnapshot {
        Objects.requireNonNull(epoch, "epoch");
        var copy = new LinkedHashMap<Hash256, ModelOffer>();
        models.values().stream().sorted((left, right) -> {
                    var byNamespace = left.namespace().compareTo(right.namespace());
                    return byNamespace != 0 ? byNamespace : left.path().compareTo(right.path());
                })
                .forEach(offer -> {
                    if (copy.putIfAbsent(offer.descriptor().modelHash(), offer) != null) {
                        throw new IllegalArgumentException("Duplicate model hash in remote catalog: "
                                + offer.descriptor().modelHash());
                    }
                });
        models = Map.copyOf(copy);
        packs = packs.stream().sorted((left, right) -> {
            var byNamespace = left.subject().namespace().compareTo(right.subject().namespace());
            return byNamespace != 0 ? byNamespace
                    : left.subject().hierarchy().compareTo(right.subject().hierarchy());
        }).toList();
    }
}
