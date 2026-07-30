package com.elfmcys.ysm.geckolib3.core.keyframe.point;

import com.elfmcys.ysm.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.ysm.geckolib3.core.keyframe.bone.BoneKeyFrame;
import com.elfmcys.ysm.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.ysm.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

public class KeyFramePoint extends AnimationPoint {
    public final BoneKeyFrame keyframe;

    public KeyFramePoint(float currentTick, BoneKeyFrame keyframe, AnimationContext context) {
        super(currentTick, keyframe.getTotalTick(), context);
        this.keyframe = keyframe;
    }

    @Override
    public Vector3f getLerpPoint(ExpressionEvaluator<MolangContext<?>> evaluator) {
        setupAnimationContext(evaluator);
        var result = keyframe.getLerpPoint(evaluator, getPercentCompleted());
        if (lastLerpResult == null) {
            lastLerpResult = new Vector3f(result);
        } else {
            lastLerpResult.set(result);
        }
        return result;
    }
}
