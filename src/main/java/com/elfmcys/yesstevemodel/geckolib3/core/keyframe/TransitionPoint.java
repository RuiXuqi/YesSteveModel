package com.elfmcys.yesstevemodel.geckolib3.core.keyframe;

import com.elfmcys.yesstevemodel.geckolib3.core.controller.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.BoneKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationMolangContext;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

public class TransitionPoint extends AnimationPoint {
    private final Vector3f offsetPoint;
    private final BoneKeyFrame dstKeyframe;

    public TransitionPoint(double currentTick, double totalTick, Vector3f offsetPoint, BoneKeyFrame dstKeyframe, AnimationContext context) {
        super(currentTick, totalTick, context);
        this.offsetPoint = offsetPoint;
        this.dstKeyframe = dstKeyframe;
    }

    @Override
    public Vector3f getLerpPoint(ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
        setupAnimationContext(evaluator);
        return dstKeyframe.getTransitionPoint(evaluator, offsetPoint, getPercentCompleted());
    }
}
