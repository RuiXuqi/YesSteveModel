package com.elfmcys.yesstevemodel.geckolib3.core.controller;

import com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;

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
    public void updateModel(List<BoneTopLevelSnapshot> modelBones, Int2ReferenceMap<List<IValue>> eventHandlers) {
        var animationControllerData = animatableEntity.getAnimationControllerData(this.name);
        if (animationControllerData != null) {
            this.bedrockAnimationController.updateModel(modelBones, animationControllerData);
            this.codedAnimationController.clear();
            this.activeController = this.bedrockAnimationController;
        } else {
            this.codedAnimationController.updateModel(modelBones, eventHandlers);
            this.bedrockAnimationController.clear();
            this.activeController = this.codedAnimationController;
        }
    }

    @Override
    public void process(AnimationEvent<T> event, ExpressionEvaluator<MolangContext<?>> evaluator, boolean allowEmitting) {
        this.activeController.process(event, evaluator, allowEmitting);
    }

    @Override
    public void visitBoneAnimationQueues(Consumer<IBoneAnimationQueue> visitor) {
        this.activeController.visitBoneAnimationQueues(visitor);
    }

    @Override
    public boolean blendRotation() {
        return this.activeController.blendRotation();
    }

    @Override
    public void clear() {
        if (this.activeController != null) {
            this.activeController.clear();
        }
    }
}
