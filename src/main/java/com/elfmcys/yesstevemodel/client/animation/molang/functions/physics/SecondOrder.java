package com.elfmcys.yesstevemodel.client.animation.molang.functions.physics;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.util.Mth;

/**
 * @author MicroCraft
 *
 * <a href="https://www.youtube.com/watch?v=KPoeNZZ6H4s">Giving Personality to Procedural Animations using Math</a>
 */
public class SecondOrder implements IPhysics {
    private final IValue argument;
    private final double k1;
    private final double k2;
    private final double k3;

    private double inputFunction = 0;
    private double lastSimulation = 0;
    private double lastSimulationDot = 0;

    public SecondOrder(IValue argument, float frequency, float coefficient, float response) {
        this.argument = argument;
        frequency = Mth.clamp(frequency, 0, 5);
        coefficient = Mth.clamp(coefficient, 0, 1);
        this.k1 = coefficient / Math.PI / frequency;
        this.k2 = 1 / (2 * Math.PI * frequency) / (2 * Math.PI * frequency);
        this.k3 = response * coefficient / 2 / Math.PI / frequency;
    }

    @Override
    public void update(ExpressionEvaluator<AnimationContext<?>> evaluator, double timeStep) {
        double input = this.argument.evalAsDouble(evaluator);

        double inputFunctionDot = (input - inputFunction) / timeStep;
        inputFunction = input;

        double tmpLastSimulation = lastSimulation + timeStep * lastSimulationDot;
        double tmpLastSimulationDot = lastSimulationDot + timeStep * (k3 * inputFunctionDot + inputFunction - tmpLastSimulation - k1 * lastSimulationDot) / k2;

        lastSimulation = tmpLastSimulation;
        lastSimulationDot = tmpLastSimulationDot;
    }

    @Override
    public double getValue() {
        return lastSimulation;
    }
}
