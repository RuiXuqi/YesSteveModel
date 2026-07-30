package com.elfmcys.ysm.model.catalog;

import java.util.Objects;

public record BackingRecoveryRequest(CatalogBackingKey backing) {
    public BackingRecoveryRequest {
        Objects.requireNonNull(backing, "backing");
    }
}
