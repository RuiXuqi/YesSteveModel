package com.elfmcys.ysm.model.catalog;

import java.nio.file.Path;
import java.util.Set;

public record SourceChangeSet(Set<Path> paths, boolean overflow) {
    public SourceChangeSet {
        paths = paths.stream().map(path -> path.toAbsolutePath().normalize())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
