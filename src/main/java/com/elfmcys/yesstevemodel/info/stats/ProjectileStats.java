package com.elfmcys.yesstevemodel.info.stats;

// Native Access
public class ProjectileStats {
    private final GeoModelStats model;
    private final ModelTextureStats texture;

    // Native Access
    public ProjectileStats(GeoModelStats model, ModelTextureStats texture) {
        this.model = model;
        this.texture = texture;
    }

    public GeoModelStats model() {
        return model;
    }

    public ModelTextureStats texture() {
        return texture;
    }
}
