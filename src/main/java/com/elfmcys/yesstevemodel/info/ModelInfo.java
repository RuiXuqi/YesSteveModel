package com.elfmcys.yesstevemodel.info;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

// Native Access
public class ModelInfo {
    @Nullable
    private final ModelMetadata metadata;
    private final ModelProperties properties;
    private final Set<String> features;
    private final String hash;

    // Native Access
    public ModelInfo(@Nullable ModelMetadata metadata, ModelProperties properties, String[] features, String hash) {
        this.metadata = metadata;
        this.properties = properties;
        this.features = ObjectSets.unmodifiable(ObjectOpenHashSet.of(features));
        this.hash = hash;
    }

    @Nullable
    public ModelMetadata metadata() {
        return metadata;
    }

    public ModelProperties properties() {
        return properties;
    }

    public Set<String> features() {
        return features;
    }

    public String hash() {
        return hash;
    }
}
