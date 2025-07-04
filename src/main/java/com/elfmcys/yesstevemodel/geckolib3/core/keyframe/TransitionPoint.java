package com.elfmcys.yesstevemodel.geckolib3.core.keyframe;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.BoneKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.event.PointType;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.util.MathUtil;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

public class TransitionPoint extends AnimationPoint {
    private final float transitionPercentProgress;
    private final Vector3f offsetPoint;
    private final BoneKeyFrame dstKeyframe;
    private final PointType type;

    public TransitionPoint(float currentTick, float transitionPercentProgress, float transitionLength, Vector3f offsetPoint, BoneKeyFrame dstKeyframe, PointType type, AnimationContext context) {
        super(currentTick, transitionLength, context);
        this.transitionPercentProgress = transitionPercentProgress;
        if (type == PointType.ROTATION) {
            this.offsetPoint = MathUtil.wrapRadians(offsetPoint);
        } else {
            this.offsetPoint = offsetPoint;
        }
        this.dstKeyframe = dstKeyframe;
        this.type = type;
    }

    @Override
    public Vector3f getLerpPoint(ExpressionEvaluator<MolangContext<?>> evaluator) {
        setupAnimationContext(evaluator);
        var result = dstKeyframe.getTransitionPoint(evaluator, offsetPoint, transitionPercentProgress);
        if (type == PointType.ROTATION) {
            return MathUtil.wrapRadians(result);
        } else {
            return result;
        }
    }

    public Vector3f getTransitionDst(ExpressionEvaluator<MolangContext<?>> evaluator) {
        var result = dstKeyframe.getTransitionPoint(evaluator, offsetPoint, 1f);
        if (type == PointType.ROTATION) {
            return MathUtil.wrapRadians(result);
        } else {
            return result;
        }
    }

    public Vector3f getTransitionOffset() {
        return offsetPoint;
    }

    public float getTransitionPercentProgress() {
        return transitionPercentProgress;
    }
}
