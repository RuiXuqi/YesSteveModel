package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.model.domain.ModelPath;

import java.util.Objects;

public record CatalogModelLocation(CatalogRootKind rootKind, ModelPath path)
        implements Comparable<CatalogModelLocation> {
    public CatalogModelLocation {
        Objects.requireNonNull(rootKind, "rootKind");
        Objects.requireNonNull(path, "path");
    }

    @Override
    public int compareTo(CatalogModelLocation other) {
        var byRoot = rootKind.compareTo(other.rootKind);
        return byRoot != 0 ? byRoot : path.compareTo(other.path);
    }
}
