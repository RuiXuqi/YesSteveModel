package com.elfmcys.yesstevemodel.client.animation.molang.functions.physics;

import net.minecraft.util.Mth;

/**
 * @author MicroCraft
 *
 * <a href="https://www.youtube.com/watch?v=KPoeNZZ6H4s">Giving Personality to Procedural Animations using Math</a>
 */
public class SecondOrder implements IPhysics {
    private double inputFunction = 0;
    private double lastSimulation = 0;
    private double lastSimulationDot = 0;
    private float[] args = new float[4];

    public SecondOrder(float input, float frequency, float coefficient, float response) {
        this.args[0] = input;
        this.args[1] = Mth.clamp(frequency, 0, 5);
        this.args[2] = Mth.clamp(coefficient, 0, 1);
        this.args[3] = response;
    }

    @Override
    public void update(double timeStep) {
        float input = args[0];
        float frequency = Mth.clamp(args[1], 0, 5);
        float coefficient = Mth.clamp(args[2], 0, 1);
        float response = args[3];

        double k1 = coefficient / Math.PI / frequency;
        double k2 = 1 / (2 * Math.PI * frequency) / (2 * Math.PI * frequency);
        double k3 = response * coefficient / 2 / Math.PI / frequency;

        double inputFunctionDot = (input - inputFunction) / timeStep;
        inputFunction = input;

        double maxTimeStep = Math.sqrt(4 * k2 + k1 * k1) - k1;
        int cycleTime = (int) Math.ceil(timeStep / maxTimeStep);
        timeStep = timeStep / cycleTime;

        for (; cycleTime > 0; cycleTime--) {
            double tmpLastSimulation = lastSimulation + timeStep * lastSimulationDot;
            double tmpLastSimulationDot = lastSimulationDot + timeStep * (k3 * inputFunctionDot + inputFunction - tmpLastSimulation - k1 * lastSimulationDot) / k2;

            lastSimulation = tmpLastSimulation;
            lastSimulationDot = tmpLastSimulationDot;
        }
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
