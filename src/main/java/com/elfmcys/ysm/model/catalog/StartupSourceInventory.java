package com.elfmcys.ysm.model.catalog;

import java.util.Map;

public record StartupSourceInventory(Map<CatalogRootIdentity, RootInventoryState> roots) {
    public StartupSourceInventory {
        roots = Map.copyOf(roots);
    }
}
