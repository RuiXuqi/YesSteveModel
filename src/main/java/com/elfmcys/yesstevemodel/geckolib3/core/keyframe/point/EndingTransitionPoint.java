package com.elfmcys.yesstevemodel.geckolib3.core.keyframe.point;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

public class EndingTransitionPoint extends AnimationPoint {
    protected final Vector3f srcPoint;

    public EndingTransitionPoint(float currentTick, float transitionLength, Vector3f srcPoint, AnimationContext context) {
        super(currentTick, transitionLength, context);
        this.srcPoint = srcPoint;
    }

    @Override
    public Vector3f getLerpPoint(ExpressionEvaluator<MolangContext<?>> evaluator) {
        if (lastLerpResult == null) {
            lastLerpResult = new Vector3f(srcPoint);
        } else {
            lastLerpResult.set(srcPoint);
        }
        return srcPoint;
    }
}
