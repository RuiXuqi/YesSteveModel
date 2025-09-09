package com.elfmcys.yesstevemodel.info;

import com.elfmcys.yesstevemodel.info.stats.PlayerMainModelStats;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import org.jetbrains.annotations.Nullable;

// Native Access
public class ModelInfo {
    @Nullable
    private final ModelMetadata metadata;
    private final ModelProperties properties;
    private final PlayerMainModelStats stats;
    private final String hash;
    private final int hashShort;

    // Native Access
    public ModelInfo(@Nullable ModelMetadata metadata, ModelProperties properties, PlayerMainModelStats stats, String hash) {
        this.metadata = metadata;
        this.properties = properties;
        this.stats = stats;
        this.hash = hash;
        this.hashShort = ModelIdUtil.getModelHashShort(hash);
    }

    @Nullable
    public ModelMetadata metadata() {
        return metadata;
    }

    public ModelProperties properties() {
        return properties;
    }

    public PlayerMainModelStats stats() {
        return stats;
    }

    public String hash() {
        return hash;
    }

    public int hashShort() {
        return hashShort;
    }
}
