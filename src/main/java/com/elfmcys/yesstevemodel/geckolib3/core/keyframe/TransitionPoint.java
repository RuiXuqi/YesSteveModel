package com.elfmcys.yesstevemodel.geckolib3.core.keyframe;

import com.elfmcys.yesstevemodel.geckolib3.core.controller.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.transition.IBlendTransition;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.BoneKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationMolangContext;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

public class TransitionPoint extends AnimationPoint {
    private final IBlendTransition transition;
    private final Vector3f offsetPoint;
    private final BoneKeyFrame dstKeyframe;
    private final boolean rotation;

    public TransitionPoint(float currentTick, IBlendTransition transition, Vector3f offsetPoint, BoneKeyFrame dstKeyframe, boolean rotation, AnimationContext context) {
        super(currentTick, transition.length(), context);
        this.transition = transition;
        this.offsetPoint = offsetPoint;
        this.dstKeyframe = dstKeyframe;
        this.rotation = rotation;
    }

    @Override
    public Vector3f getLerpPoint(ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
        setupAnimationContext(evaluator);
        return dstKeyframe.getTransitionPoint(evaluator, offsetPoint, rotation, transition.get(currentTick));
    }
}
