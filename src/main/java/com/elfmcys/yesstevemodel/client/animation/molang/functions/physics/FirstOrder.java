package com.elfmcys.yesstevemodel.client.animation.molang.functions.physics;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;

public class FirstOrder implements IPhysics {
    private final IValue argument;
    private final float response;
    private double lastSimulation = 0;

    public FirstOrder(IValue argument, float response) {
        this.argument = argument;
        this.response = response;
    }

    @Override
    public void update(ExpressionEvaluator<AnimationContext<?>> evaluator, double timeStep) {
        double input = this.argument.evalAsDouble(evaluator);
        lastSimulation = (1 - timeStep / this.response) * lastSimulation + timeStep / this.response * input;
    }

    @Override
    public double getValue() {
        return lastSimulation;
    }
}
