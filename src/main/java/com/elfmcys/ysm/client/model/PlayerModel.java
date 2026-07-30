package com.elfmcys.ysm.client.model;

import com.elfmcys.ysm.client.animation.condition.ConditionManager;
import com.elfmcys.ysm.client.animation.condition.FPArmConditionManager;
import com.elfmcys.ysm.client.compat.touhoulittlemaid.client.TlmClientCompat;
import com.elfmcys.ysm.client.controller.collections.FPArmControllerCollection;
import com.elfmcys.ysm.client.controller.collections.PlayerControllerCollection;
import com.elfmcys.ysm.client.entity.CustomFirstPersonArmEntity;
import com.elfmcys.ysm.client.entity.CustomPlayerEntity;
import com.elfmcys.ysm.geckolib3.core.builder.Animation;
import com.elfmcys.ysm.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.ysm.util.FifoHashMap;
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
    private final Consumer<CustomFirstPersonArmEntity> fpArmControllerFactory;
    private final Object maidControllerFactory;

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
        this.fpArmControllerFactory = FPArmControllerCollection.build(this, assets);
        this.maidControllerFactory = TlmClientCompat.buildControllerFactory(this, assets);
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

    public Consumer<CustomFirstPersonArmEntity> fpArmControllerFactory() {
        return fpArmControllerFactory;
    }

    public Object maidControllerFactory() {
        return maidControllerFactory;
    }
}
