package com.elfmcys.ysm.info;

import com.elfmcys.ysm.info.stats.PlayerMainModelStats;
import com.elfmcys.ysm.util.ModelIdUtil;
import org.jetbrains.annotations.Nullable;

// Native Access
public class ModelInfo {
    @Nullable
    private final ModelMetadata metadata;
    private final ModelProperties properties;
    private final PlayerMainModelStats stats;
    private final int formatVer;
    private final String hash;
    private final String extra;
    private final long timestamp;
    private final String rnd;
    private final int hashShort;

    // Native Access
    public ModelInfo(@Nullable ModelMetadata metadata, ModelProperties properties, PlayerMainModelStats stats, int formatVer, String hash, String extra, long timestamp, String rnd) {
        this.metadata = metadata;
        this.properties = properties;
        this.stats = stats;
        this.formatVer = formatVer;
        this.hash = hash;
        this.extra = extra;
        this.timestamp = timestamp;
        this.rnd = rnd;
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

    public int formatVer() {
        return formatVer;
    }

    public String hash() {
        return hash;
    }

    public String extra() {
        return extra;
    }

    public long timestamp() {
        return timestamp;
    }

    public String rnd() {
        return rnd;
    }

    public int hashShort() {
        return hashShort;
    }
}
