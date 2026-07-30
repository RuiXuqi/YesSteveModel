package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.model.domain.ModelHash;

import java.util.Objects;

public record ConvertedObjectKey(ConversionProfileId profile, ModelHash modelHash)
        implements Comparable<ConvertedObjectKey> {
    public ConvertedObjectKey {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(modelHash, "modelHash");
    }

    @Override
    public int compareTo(ConvertedObjectKey other) {
        var profileOrder = profile.compareTo(other.profile);
        return profileOrder != 0 ? profileOrder : modelHash.compareTo(other.modelHash);
    }

    @Override
    public String toString() {
        return profile + "/" + modelHash;
    }
}
