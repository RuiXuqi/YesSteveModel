package com.elfmcys.yesstevemodel.client.model;

import com.elfmcys.yesstevemodel.client.animation.condition.ConditionManager;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.util.FifoHashMap;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class PlayerModel {
    private final GeoModel mainModel;
    private final GeoModel armModel;
    private final Map<String, Animation> animations;
    private final ConditionManager conditionManager;
    private final Map<String, AnimationControllerData> animationControllers;
    private final FifoHashMap<String, ResourceLocation> textures;
    private final String defaultTextureName;
    private final ResourceLocation defaultTexture;

    public PlayerModel(GeoModel mainModel, GeoModel armModel, Map<String, Animation> animations, ConditionManager conditionManager, Map<String, AnimationControllerData> animationControllers, FifoHashMap<String, ResourceLocation> textures, String defaultTextureName, ResourceLocation defaultTexture) {
        this.mainModel = mainModel;
        this.armModel = armModel;
        this.animations = animations;
        this.conditionManager = conditionManager;
        this.animationControllers = animationControllers;
        this.textures = textures;
        this.defaultTextureName = defaultTextureName;
        this.defaultTexture = defaultTexture;
    }

    public GeoModel mainModel() {
        return mainModel;
    }

    public GeoModel armModel() {
        return armModel;
    }

    public Map<String, Animation> animations() {
        return animations;
    }

    public ConditionManager conditionManager() {
        return conditionManager;
    }

    public Map<String, AnimationControllerData> animationControllers() {
        return animationControllers;
    }

    public FifoHashMap<String, ResourceLocation> textures() {
        return textures;
    }

    public String defaultTextureName() {
        return defaultTextureName;
    }

    public ResourceLocation defaultTexture() {
        return defaultTexture;
    }
}
