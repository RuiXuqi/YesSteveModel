package com.elfmcys.yesstevemodel.info;

import com.elfmcys.yesstevemodel.info.stats.ProjectileStats;
import com.elfmcys.yesstevemodel.info.type.ProjectileType;
import com.elfmcys.yesstevemodel.info.stats.GeoModelStats;
import com.elfmcys.yesstevemodel.info.stats.ModelTextureStats;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

import java.util.Map;

// Native Access
public class ModelStats {
    private final GeoModelStats playerModel;
    private final GeoModelStats armModel;
    private final Map<String, ModelTextureStats> textures;
    private final Map<ProjectileType, ProjectileStats> projectileStats;

    // Native Access
    public ModelStats(GeoModelStats[] models, Map<String, ModelTextureStats> textures, Map<ProjectileType, ProjectileStats> projectileStats) {
        this.playerModel = models[0];
        this.armModel = models[1];
        this.textures = Object2ReferenceMaps.unmodifiable(new Object2ReferenceOpenHashMap<>(textures));
        this.projectileStats = Reference2ReferenceMaps.unmodifiable(new Reference2ReferenceOpenHashMap<>(projectileStats));
    }

    public GeoModelStats playerModel() {
        return playerModel;
    }

    public GeoModelStats armModel() {
        return armModel;
    }

    public Map<String, ModelTextureStats> textures() {
        return textures;
    }

    public Map<ProjectileType, ProjectileStats> projectileStats() {
        return projectileStats;
    }
}
