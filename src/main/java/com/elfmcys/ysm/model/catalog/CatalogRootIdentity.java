package com.elfmcys.ysm.model.catalog;

import java.nio.file.Path;
import java.util.Objects;

public record CatalogRootIdentity(CatalogRootKind rootKind, Path canonicalAbsoluteRoot,
                                  String fileKey) implements Comparable<CatalogRootIdentity> {
    public CatalogRootIdentity {
        Objects.requireNonNull(rootKind, "rootKind");
        canonicalAbsoluteRoot = Objects.requireNonNull(canonicalAbsoluteRoot,
                "canonicalAbsoluteRoot").toAbsolutePath().normalize();
        fileKey = Objects.requireNonNullElse(fileKey, "");
    }

    @Override
    public int compareTo(CatalogRootIdentity other) {
        var kind = rootKind.compareTo(other.rootKind);
        if (kind != 0) {
            return kind;
        }
        var path = canonicalAbsoluteRoot.toString()
                .compareTo(other.canonicalAbsoluteRoot.toString());
        return path != 0 ? path : fileKey.compareTo(other.fileKey);
    }
}
