package com.elfmcys.yesstevemodel.client.model.data;

import com.elfmcys.yesstevemodel.client.texture.NativeTexture;
import com.elfmcys.yesstevemodel.geckolib3.file.AnimationControllerFile;
import com.elfmcys.yesstevemodel.geckolib3.file.AnimationFile;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;

// Native Access
public class VehicleModelData {
    private final GeoModel geoModel;
    private final AnimationFile animationFile;
    private final AnimationControllerFile controllerFile;
    private final NativeTexture texture;

    // Native Access
    public VehicleModelData(GeoModel geoModel, AnimationFile animationFile, AnimationControllerFile controllerFile, NativeTexture texture) {
        this.geoModel = geoModel;
        this.animationFile = animationFile;
        this.controllerFile = controllerFile;
        this.texture = texture;
    }

    public GeoModel geoModel() {
        return geoModel;
    }

    public AnimationFile animationFile() {
        return animationFile;
    }

    public AnimationControllerFile controllerFile() {
        return controllerFile;
    }

    public NativeTexture texture() {
        return texture;
    }
}
