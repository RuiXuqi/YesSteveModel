package com.elfmcys.ysm.model.catalog;

import java.nio.file.Path;
import java.util.Objects;

public record SourceObservation(ModelSourceKey key, Path absolutePath, SourceStamp stamp,
                                long estimatedBytes) {
    public SourceObservation {
        Objects.requireNonNull(key, "key");
        absolutePath = Objects.requireNonNull(absolutePath, "absolutePath")
                .toAbsolutePath().normalize();
        Objects.requireNonNull(stamp, "stamp");
        if (estimatedBytes < 0) {
            throw new IllegalArgumentException("estimatedBytes must not be negative");
        }
    }
}
