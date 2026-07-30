package com.elfmcys.ysm.model.catalog;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;

public record PackObservation(ModelPackSourceKey key, Path directory,
                              SourceStamp.File manifestStamp,
                              @Nullable SourceStamp.File coverStamp) {
    public PackObservation {
        Objects.requireNonNull(key, "key");
        directory = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
        Objects.requireNonNull(manifestStamp, "manifestStamp");
    }
}
