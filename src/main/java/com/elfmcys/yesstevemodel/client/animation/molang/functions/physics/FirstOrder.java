package com.elfmcys.yesstevemodel.client.animation.molang.functions.physics;

public class FirstOrder implements IPhysics {
    private float input;
    private float response;
    private double lastSimulation = 0;

    public FirstOrder(float input, float response) {
        this.input = input;
        this.response = response;
    }

    @Override
    public void update(double timeStep) {
        double step = 1 / 60d;
        lastSimulation = (1 - step / response) * lastSimulation + step / response * input;
    }

    @Override
    public void setArgs(float... args) {
        this.input = args[0];
        this.response = args[1];
    }

    @Override
    public double getValue() {
        return lastSimulation;
    }
}
