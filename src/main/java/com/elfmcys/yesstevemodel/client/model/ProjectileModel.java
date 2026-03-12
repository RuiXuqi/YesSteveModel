package com.elfmcys.yesstevemodel.client.model;

import com.elfmcys.yesstevemodel.client.controller.collections.ProjectileControllerCollection;
import com.elfmcys.yesstevemodel.client.entity.CustomProjectileEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.util.function.Consumer;

public class ProjectileModel {
    private final GeoModel model;
    private final Object2ReferenceMap<String, Animation> animations;
    private final Object2ReferenceMap<String, AnimationControllerData> controllers;
    private final AbstractTexture texture;
    private final Consumer<CustomProjectileEntity> controllerFactory;

    public ProjectileModel(GeoModel model, Object2ReferenceMap<String, Animation> animations, Object2ReferenceMap<String, AnimationControllerData> controllers, AbstractTexture texture, CommonAsset assets) {
        this.model = model;
        this.animations = animations;
        this.controllers = controllers;
        this.texture = texture;
        this.controllerFactory = ProjectileControllerCollection.build(this, assets);
    }

    public GeoModel model() {
        return model;
    }

    public Object2ReferenceMap<String, Animation> animations() {
        return animations;
    }

    public Object2ReferenceMap<String, AnimationControllerData> controllers() {
        return controllers;
    }

    public AbstractTexture texture() {
        return texture;
    }

    public Consumer<CustomProjectileEntity> controllerFactory() {
        return controllerFactory;
    }
}
