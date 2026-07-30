package com.elfmcys.ysm.model.catalog;

import java.nio.file.Path;
import java.util.Objects;

public record ModelCatalogSource(CatalogRootKind rootKind, Path path, boolean createIfMissing) {
    public ModelCatalogSource {
        Objects.requireNonNull(rootKind, "rootKind");
        Objects.requireNonNull(path, "path");
    }
}
