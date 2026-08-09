package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.model.catalog.ModelSourceKey;
import com.elfmcys.ysm.model.catalog.SourceStamp;
import com.elfmcys.ysm.model.domain.Hash256;

import java.util.Objects;

public record ConvertedSourceIndex(ModelSourceKey sourceKey, SourceStamp lastObservedStamp,
                                   ConversionProfileId profile, Hash256 modelHash,
                                   Hash256 descriptorHash) {
    public ConvertedSourceIndex {
        Objects.requireNonNull(sourceKey, "sourceKey");
        Objects.requireNonNull(lastObservedStamp, "lastObservedStamp");
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(modelHash, "modelHash");
        Objects.requireNonNull(descriptorHash, "descriptorHash");
    }
}
