package com.elfmcys.ysm.client.model;

import com.elfmcys.ysm.client.animation.condition.ConditionManager;
import com.elfmcys.ysm.client.animation.condition.FPArmConditionManager;
import com.elfmcys.ysm.client.compat.touhoulittlemaid.client.TlmClientCompat;
import com.elfmcys.ysm.client.controller.collections.FPArmControllerCollection;
import com.elfmcys.ysm.client.controller.collections.PlayerControllerCollection;
import com.elfmcys.ysm.client.entity.CustomFirstPersonArmEntity;
import com.elfmcys.ysm.client.entity.CustomPlayerEntity;
import com.elfmcys.ysm.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.ysm.util.FifoHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;

import java.util.Objects;
import java.util.function.Consumer;

public class PlayerModelResources implements RenderTargetResources {
    private final FifoHashMap<String, PlayerModelVariant> variants;
    private final AnimationStore animations;
    private final AnimationStore fpArmAnimations;
    private final ConditionManager conditionManager;
    private final FPArmConditionManager fpArmConditionManager;
    private final Object2ReferenceMap<String, AnimationControllerData> animationControllers;
    private final String defaultTextureName;
    private final PlayerModelVariant defaultVariant;
    private final Consumer<CustomPlayerEntity> playerControllerFactory;
    private final Consumer<CustomFirstPersonArmEntity> fpArmControllerFactory;
    private final Object maidControllerFactory;

    public PlayerModelResources(FifoHashMap<String, PlayerModelVariant> variants, AnimationStore animations, AnimationStore fpArmAnimations,
                       ConditionManager conditionManager, FPArmConditionManager fpArmConditionManager, Object2ReferenceMap<String, AnimationControllerData> animationControllers,
                       String defaultTextureName, CommonAsset assets) {
        this.variants = variants;
        this.animations = animations;
        this.fpArmAnimations = fpArmAnimations;
        this.conditionManager = conditionManager;
        this.animationControllers = animationControllers;
        this.fpArmConditionManager = fpArmConditionManager;
        this.defaultTextureName = defaultTextureName;
        this.defaultVariant = Objects.requireNonNull(variants.get(defaultTextureName), "defaultVariant");
        this.playerControllerFactory = PlayerControllerCollection.build(this, assets);
        this.fpArmControllerFactory = FPArmControllerCollection.build(this, assets);
        this.maidControllerFactory = TlmClientCompat.buildControllerFactory(this, assets);
    }

    public FifoHashMap<String, PlayerModelVariant> variants() {
        return variants;
    }

    public AnimationStore animations() {
        return animations;
    }

    public AnimationStore fpArmAnimations() {
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

    public String defaultTextureName() {
        return defaultTextureName;
    }

    public PlayerModelVariant defaultVariant() {
        return defaultVariant;
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
