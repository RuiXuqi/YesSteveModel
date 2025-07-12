package com.elfmcys.yesstevemodel.geckolib3.core.keyframe;

import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.BoneKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.TransitionKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.util.MathUtil;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

public class TransitionPoint extends AnimationPoint {
    protected final float transitionPercentProgress;
    protected final Vector3f offsetPoint;
    protected final TransitionKeyFrame dstKeyframe;

    public TransitionPoint(float currentTick, float transitionPercentProgress, float transitionLength, Vector3f offsetPoint, TransitionKeyFrame dstKeyframe, AnimationContext context) {
        super(currentTick, transitionLength, context);
        this.transitionPercentProgress = transitionPercentProgress;
        this.offsetPoint = offsetPoint;
        this.dstKeyframe = dstKeyframe;
    }

    @Override
    public Vector3f getLerpPoint(ExpressionEvaluator<MolangContext<?>> evaluator) {
        setupAnimationContext(evaluator);
        if (BoneKeyFrame.isBegin(transitionPercentProgress)) {
            return offsetPoint;
        }
        var dst = dstKeyframe.getTransitionDst(evaluator);
        if (BoneKeyFrame.isEnd(transitionPercentProgress)) {
            return dst;
        }
        return MathUtil.lerpValues(transitionPercentProgress, offsetPoint, dst);
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
