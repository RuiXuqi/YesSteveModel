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

    private double inputFunction = 0;
    private double lastSimulation = 0;
    private double lastSimulationDot = 0;
    private float[] args = new float[3];

    public SecondOrder(IValue argument, float frequency, float coefficient, float response) {
        this.argument = argument;
        this.args[0] = Mth.clamp(frequency, 0, 5);
        this.args[1] = Mth.clamp(coefficient, 0, 1);
        this.args[2] = response;
    }

    @Override
    public void update(ExpressionEvaluator<AnimationContext<?>> evaluator, double timeStep) {
        double input = this.argument.evalAsDouble(evaluator);
        float frequency = Mth.clamp(args[0], 0, 5);
        float coefficient = Mth.clamp(args[1], 0, 1);
        float response = args[2];

        double k1 = coefficient / Math.PI / frequency;
        double k2 = 1 / (2 * Math.PI * frequency) / (2 * Math.PI * frequency);
        double k3 = response * coefficient / 2 / Math.PI / frequency;

        double inputFunctionDot = (input - inputFunction) / timeStep;
        inputFunction = input;

        double tmpLastSimulation = lastSimulation + timeStep * lastSimulationDot;
        double tmpLastSimulationDot = lastSimulationDot + timeStep * (k3 * inputFunctionDot + inputFunction - tmpLastSimulation - k1 * lastSimulationDot) / k2;

        lastSimulation = tmpLastSimulation;
        lastSimulationDot = tmpLastSimulationDot;
    }

    @Override
    public void setArgs(float... args) {
        this.args = args;
    }

    @Override
    public double getValue() {
        return lastSimulation;
    }
}
