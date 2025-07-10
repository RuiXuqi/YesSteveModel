package com.elfmcys.yesstevemodel.geckolib3.core.controller;

import com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;

import java.util.List;
import java.util.function.Consumer;

public class HybridAnimationController<T extends AnimatableEntity<?>> implements IAnimationController<T> {
    private final String name;
    private final T animatableEntity;
    private final CodedAnimationController<T> codedAnimationController;
    private final BedrockAnimationController<T> bedrockAnimationController;

    private IAnimationController<T> activeController;

    public HybridAnimationController(T animatableEntity, String name, float transitionLengthTicks, IAnimationPredicate<T> animationPredicate) {
        this(animatableEntity, name, transitionLengthTicks, animationPredicate, false);
    }

    @Deprecated
    public HybridAnimationController(T animatableEntity, String name, float transitionLengthTicks, IAnimationPredicate<T> animationPredicate, boolean blendRotation) {
        this.name = name;
        this.animatableEntity = animatableEntity;
        this.codedAnimationController = new CodedAnimationController<>(animatableEntity, name, transitionLengthTicks, animationPredicate, blendRotation);
        this.bedrockAnimationController = new BedrockAnimationController<>(animatableEntity, name, transitionLengthTicks);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getState() {
        return this.activeController.getState();
    }

    @Override
    public void updateModelBones(List<BoneTopLevelSnapshot> modelBones) {
        var animationControllerData = animatableEntity.getAnimationControllerData(this.name);
        if (animationControllerData != null) {
            this.bedrockAnimationController.updateModelBones(modelBones, animationControllerData);
            this.activeController = this.bedrockAnimationController;
        } else {
            this.codedAnimationController.updateModelBones(modelBones);
            this.activeController = this.codedAnimationController;
        }
    }

    @Override
    public void process(AnimationEvent<T> event, ExpressionEvaluator<MolangContext<?>> evaluator) {
        this.activeController.process(event, evaluator);
    }

    @Override
    public void visitBoneAnimationQueues(Consumer<IBoneAnimationQueue> visitor) {
        this.activeController.visitBoneAnimationQueues(visitor);
    }

    @Override
    public boolean blendRotation() {
        return this.activeController.blendRotation();
    }
}
