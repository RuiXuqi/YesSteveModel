package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.model.storage.ModelBackingIdentity;

import java.util.Objects;

public record CatalogBackingKey(CatalogModelLocation location,
                                ModelBackingIdentity identity) {
    public CatalogBackingKey {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(identity, "identity");
    }
}
