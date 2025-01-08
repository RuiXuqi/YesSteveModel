package com.elfmcys.yesstevemodel.client.animation.molang.functions.physics;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;

public interface IPhysics {
    void update(ExpressionEvaluator<AnimationContext<?>> evaluator, double timeStep);

    void setArgs(float... args);

    double getValue();
}
