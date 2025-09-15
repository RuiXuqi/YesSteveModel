package com.elfmcys.yesstevemodel.client.model;

import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class ProjectileModel {
    private final GeoModel model;
    private final Map<String, Animation> animations;
    private final Map<String, AnimationControllerData> controllers;
    private final ResourceLocation texture;

    public ProjectileModel(GeoModel model, Map<String, Animation> animations, Map<String, AnimationControllerData> controllers, ResourceLocation texture) {
        this.model = model;
        this.animations = animations;
        this.controllers = controllers;
        this.texture = texture;
    }

    public GeoModel model() {
        return model;
    }

    public Map<String, Animation> animations() {
        return animations;
    }

    public Map<String, AnimationControllerData> controllers() {
        return controllers;
    }

    public ResourceLocation texture() {
        return texture;
    }
}
