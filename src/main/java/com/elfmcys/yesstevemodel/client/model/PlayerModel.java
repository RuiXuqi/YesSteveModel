package com.elfmcys.yesstevemodel.client.model;

import com.elfmcys.yesstevemodel.client.animation.condition.ConditionManager;
import com.elfmcys.yesstevemodel.client.animation.condition.FPArmConditionManager;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.CustomYsmMaidEntity;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.MaidControllerCollection;
import com.elfmcys.yesstevemodel.client.controller.collections.FPArmControllerCollection;
import com.elfmcys.yesstevemodel.client.controller.collections.PlayerControllerCollection;
import com.elfmcys.yesstevemodel.client.entity.CustomFirstPersonArmEntity;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.util.FifoHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.util.function.Consumer;

public class PlayerModel {
    private final GeoModel mainModel;
    private final GeoModel armModel;
    private final Object2ReferenceMap<String, Animation> animations;
    private final Object2ReferenceMap<String, Animation> fpArmAnimations;
    private final ConditionManager conditionManager;
    private final FPArmConditionManager fpArmConditionManager;
    private final Object2ReferenceMap<String, AnimationControllerData> animationControllers;
    private final FifoHashMap<String, ? extends AbstractTexture> textures;
    private final String defaultTextureName;
    private final AbstractTexture defaultTexture;
    private final Consumer<CustomPlayerEntity> playerControllerFactory;
    private final Consumer<CustomYsmMaidEntity> maidControllerFactory;
    private final Consumer<CustomFirstPersonArmEntity> fpArmControllerFactory;

    public PlayerModel(GeoModel mainModel, GeoModel armModel, Object2ReferenceMap<String, Animation> animations, Object2ReferenceMap<String, Animation> fpArmAnimations,
                       ConditionManager conditionManager, FPArmConditionManager fpArmConditionManager, Object2ReferenceMap<String, AnimationControllerData> animationControllers,
                       FifoHashMap<String, ? extends AbstractTexture> textures, String defaultTextureName, AbstractTexture defaultTexture, CommonAsset assets) {
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
        this.playerControllerFactory = PlayerControllerCollection.build(this, assets);
        this.maidControllerFactory = MaidControllerCollection.build(this, assets);
        this.fpArmControllerFactory = FPArmControllerCollection.build(this, assets);
    }

    public GeoModel mainModel() {
        return mainModel;
    }

    public GeoModel armModel() {
        return armModel;
    }

    public Object2ReferenceMap<String, Animation> animations() {
        return animations;
    }

    public Object2ReferenceMap<String, Animation> fpArmAnimations() {
        return fpArmAnimations;
    }

    public ConditionManager conditionManager() {
        return conditionManager;
    }

    public FPArmConditionManager fpArmConditionManager() {
        return fpArmConditionManager;
    }

    public Object2ReferenceMap<String, AnimationControllerData> animationControllers() {
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

    public Consumer<CustomPlayerEntity> playerControllerFactory() {
        return playerControllerFactory;
    }

    public Consumer<CustomYsmMaidEntity> maidControllerFactory() {
        return maidControllerFactory;
    }

    public Consumer<CustomFirstPersonArmEntity> fpArmControllerFactory() {
        return fpArmControllerFactory;
    }
}
