package com.elfmcys.yesstevemodel.geckolib3.core.keyframe.point;

import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.TransitionKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.util.MathUtil;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

public class BeginningTransitionPoint extends AnimationPoint {
    protected final float transitionPercentProgress;
    protected final Vector3f offsetPoint;
    protected final TransitionKeyFrame dstKeyframe;
    protected final boolean isRotation;

    public BeginningTransitionPoint(float currentTick, float transitionPercentProgress, float transitionLength, Vector3f offsetPoint, TransitionKeyFrame dstKeyframe, boolean isRotation, AnimationContext context) {
        super(currentTick, transitionLength, context);
        this.transitionPercentProgress = transitionPercentProgress;
        this.offsetPoint = offsetPoint;
        this.dstKeyframe = dstKeyframe;
        this.isRotation = isRotation;
    }

    @Override
    public Vector3f getLerpPoint(ExpressionEvaluator<MolangContext<?>> evaluator) {
        setupAnimationContext(evaluator);
        var dst = dstKeyframe.getTransitionDst(evaluator);
        if (isRotation) {
            MathUtil.lerpRotationValues(transitionPercentProgress, offsetPoint, dst, dst);
        } else {
            MathUtil.lerpValues(transitionPercentProgress, offsetPoint, dst, dst);
        }
        if (lastLerpResult == null) {
            lastLerpResult = new Vector3f(dst);
        } else {
            lastLerpResult.set(dst);
        }
        return dst;
    }

    public Vector3f getTransitionDst(ExpressionEvaluator<MolangContext<?>> evaluator) {
        setupAnimationContext(evaluator);
        return dstKeyframe.getTransitionDst(evaluator);
    }

    public Vector3f getTransitionOffset() {
        return offsetPoint;
    }

    public float getTransitionPercentProgress() {
        return transitionPercentProgress;
    }
}
