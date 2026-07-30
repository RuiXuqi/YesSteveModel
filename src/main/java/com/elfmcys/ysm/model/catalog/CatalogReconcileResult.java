package com.elfmcys.ysm.model.catalog;

import java.util.Set;

public record CatalogReconcileResult(ReloadableCatalogSnapshot snapshot,
                                     Set<CatalogBackingKey> touchedDirectBackings,
                                     Set<CatalogBackingKey> revalidatedBackings,
                                     ReloadStats stats) {
    public CatalogReconcileResult {
        touchedDirectBackings = Set.copyOf(touchedDirectBackings);
        revalidatedBackings = Set.copyOf(revalidatedBackings);
    }
}
