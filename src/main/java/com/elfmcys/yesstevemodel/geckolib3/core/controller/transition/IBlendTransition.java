package com.elfmcys.yesstevemodel.geckolib3.core.controller.transition;

public interface IBlendTransition {
    double get(double tick);

    /**
     * Tick
     */
    double length();

    default IBlendTransition startNew() {
        return this;
    }
}
