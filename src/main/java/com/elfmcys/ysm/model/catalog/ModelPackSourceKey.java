package com.elfmcys.ysm.model.catalog;

import java.util.Objects;

public record ModelPackSourceKey(CatalogRootIdentity root, String hierarchy)
        implements Comparable<ModelPackSourceKey> {
    public ModelPackSourceKey {
        Objects.requireNonNull(root, "root");
        hierarchy = Objects.requireNonNullElse(hierarchy, "");
    }

    @Override
    public int compareTo(ModelPackSourceKey other) {
        var rootOrder = root.compareTo(other.root);
        return rootOrder != 0 ? rootOrder : hierarchy.compareTo(other.hierarchy);
    }
}
