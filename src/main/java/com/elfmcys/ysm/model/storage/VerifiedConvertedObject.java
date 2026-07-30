package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.model.domain.ModelDescriptor;

import java.nio.file.Path;
import java.util.Objects;

public record VerifiedConvertedObject(ConvertedObjectKey key, Path file,
                                      ModelDescriptor descriptor) {
    public VerifiedConvertedObject {
        Objects.requireNonNull(key, "key");
        file = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
        Objects.requireNonNull(descriptor, "descriptor");
    }
}
