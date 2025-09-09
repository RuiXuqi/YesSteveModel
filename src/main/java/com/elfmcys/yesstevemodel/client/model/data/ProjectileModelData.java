package com.elfmcys.yesstevemodel.client.model.data;

import com.elfmcys.yesstevemodel.client.texture.NativeTexture;
import com.elfmcys.yesstevemodel.geckolib3.file.AnimationFile;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;

// Native Access
public class ProjectileModelData {
    private final GeoModel geoModel;
    private final AnimationFile animationFile;
    private final NativeTexture texture;

    // Native Access
    public ProjectileModelData(GeoModel geoModel, AnimationFile animationFile, NativeTexture texture) {
        this.geoModel = geoModel;
        this.animationFile = animationFile;
        this.texture = texture;
    }

    public GeoModel geoModel() {
        return geoModel;
    }

    public AnimationFile animationFile() {
        return animationFile;
    }

    public NativeTexture texture() {
        return texture;
    }
}
