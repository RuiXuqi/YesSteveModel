package com.elfmcys.ysm.model.source;

import java.util.List;
import java.util.Objects;

public record ResolvedCatalog(CatalogCursor cursor, List<ModelOffer> models,
                              List<PackOffer> packs) {
    public ResolvedCatalog {
        Objects.requireNonNull(cursor, "cursor");
        models = List.copyOf(models);
        packs = List.copyOf(packs);
        if (models.stream().anyMatch(offer -> !cursor.sourceId().equals(offer.sourceId()))
                || packs.stream().anyMatch(offer -> !cursor.sourceId().equals(offer.sourceId()))) {
            throw new IllegalArgumentException("Catalog entries must belong to the cursor source");
        }
    }
}
