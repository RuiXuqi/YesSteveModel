package com.elfmcys.ysm.model.catalog;

import java.util.Objects;
import java.util.Set;

public record ReloadableCatalogTransition(ReloadableCatalogSnapshot previous,
                                          ReloadableCatalogSnapshot current,
                                          Set<CatalogBackingKey> touchedDirectBackings,
                                          Set<CatalogBackingKey> revalidatedBackings,
                                          ReloadStats stats) {
    public ReloadableCatalogTransition {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");
        touchedDirectBackings = Set.copyOf(touchedDirectBackings);
        revalidatedBackings = Set.copyOf(revalidatedBackings);
        Objects.requireNonNull(stats, "stats");
    }

    public static ReloadableCatalogTransition initial(ReloadableCatalogSnapshot current) {
        return new ReloadableCatalogTransition(current, current, Set.of(), Set.of(),
                ReloadStats.empty());
    }
}
