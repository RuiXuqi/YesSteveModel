package com.elfmcys.yesstevemodel.client.model;

import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class ProjectileModel {
    private final GeoModel model;

    private final Map<String, Animation> animations;

    private final ResourceLocation texture;

    public ProjectileModel(GeoModel model, Map<String, Animation> animations, ResourceLocation texture) {
        this.model = model;
        this.animations = animations;
        this.texture = texture;
    }

    public GeoModel model() {
        return model;
    }

    public Map<String, Animation> animations() {
        return animations;
    }

    public ResourceLocation texture() {
        return texture;
    }
}
