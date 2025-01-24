package com.elfmcys.yesstevemodel.geckolib3.core.controller.transition;

public class LinearBlendTransition implements IBlendTransition {
    private final double ticks;

    // Native Access
    @SuppressWarnings("unused")
    public LinearBlendTransition(float length) {
        this.ticks = length * 20;
    }

    @Override
    public double get(double tick) {
        return ticks != 0 ? tick / ticks : 1;
    }

    @Override
    public double length() {
        return ticks;
    }
}
