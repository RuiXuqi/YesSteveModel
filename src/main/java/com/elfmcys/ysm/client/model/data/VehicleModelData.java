package com.elfmcys.ysm.client.model.data;

import com.elfmcys.ysm.client.model.AnimationStore;
import com.elfmcys.ysm.client.texture.CustomPBRTextureSet;
import com.elfmcys.ysm.geckolib3.file.AnimationControllerFile;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoModel;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record VehicleModelData(GeoModel geoModel, AnimationStore animations,
                               @Nullable AnimationControllerFile controllerFile,
                               CustomPBRTextureSet texture) implements RenderTargetData {
    public VehicleModelData {
        Objects.requireNonNull(geoModel, "geoModel");
        Objects.requireNonNull(animations, "animations");
        Objects.requireNonNull(texture, "texture");
    }
}
