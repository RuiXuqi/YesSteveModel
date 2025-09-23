package com.elfmcys.yesstevemodel.client.model;

import com.elfmcys.yesstevemodel.client.animation.condition.ConditionManager;
import com.elfmcys.yesstevemodel.client.animation.condition.FPArmConditionManager;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.util.FifoHashMap;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.util.Map;

public class PlayerModel {
    private final GeoModel mainModel;
    private final GeoModel armModel;
    private final Map<String, Animation> animations;
    private final Map<String, Animation> fpArmAnimations;
    private final ConditionManager conditionManager;
    private final FPArmConditionManager fpArmConditionManager;
    private final Map<String, AnimationControllerData> animationControllers;
    private final FifoHashMap<String, ? extends AbstractTexture> textures;
    private final String defaultTextureName;
    private final AbstractTexture defaultTexture;

    public PlayerModel(GeoModel mainModel, GeoModel armModel, Map<String, Animation> animations, Map<String, Animation> fpArmAnimations,
                       ConditionManager conditionManager, FPArmConditionManager fpArmConditionManager, Map<String, AnimationControllerData> animationControllers,
                       FifoHashMap<String, ? extends AbstractTexture> textures, String defaultTextureName, AbstractTexture defaultTexture) {
        this.mainModel = mainModel;
        this.armModel = armModel;
        this.animations = animations;
        this.fpArmAnimations = fpArmAnimations;
        this.conditionManager = conditionManager;
        this.animationControllers = animationControllers;
        this.fpArmConditionManager = fpArmConditionManager;
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

    public Map<String, Animation> fpArmAnimations() {
        return fpArmAnimations;
    }

    public ConditionManager conditionManager() {
        return conditionManager;
    }

    public FPArmConditionManager fpArmConditionManager() {
        return fpArmConditionManager;
    }

    public Map<String, AnimationControllerData> animationControllers() {
        return animationControllers;
    }

    public FifoHashMap<String, ? extends AbstractTexture> textures() {
        return textures;
    }

    public String defaultTextureName() {
        return defaultTextureName;
    }

    public AbstractTexture defaultTexture() {
        return defaultTexture;
    }
}
