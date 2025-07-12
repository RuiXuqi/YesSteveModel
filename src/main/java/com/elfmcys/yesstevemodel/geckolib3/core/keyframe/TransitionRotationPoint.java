package com.elfmcys.yesstevemodel.geckolib3.core.keyframe;

import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.BoneKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.TransitionKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.util.MathUtil;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

public class TransitionRotationPoint extends TransitionPoint {
    public TransitionRotationPoint(float currentTick, float transitionPercentProgress, float transitionLength, Vector3f offsetPoint, TransitionKeyFrame dstKeyframe, AnimationContext context) {
        super(currentTick, transitionPercentProgress, transitionLength, offsetPoint, dstKeyframe, context);
    }

    @Override
    public Vector3f getLerpPoint(ExpressionEvaluator<MolangContext<?>> evaluator) {
        setupAnimationContext(evaluator);
        var dst = dstKeyframe.getTransitionDst(evaluator);
        if (BoneKeyFrame.isEnd(transitionPercentProgress)) {
            return dst;
        }
        var offsetPoint = new Vector3f(dst);
        dst.sub(MathUtil.wrapRadians(offsetPoint.sub(this.offsetPoint)), offsetPoint);
        if (BoneKeyFrame.isBegin(transitionPercentProgress)) {
            return offsetPoint;
        }
        return MathUtil.lerpValues(transitionPercentProgress, offsetPoint, dst);
    }
}
