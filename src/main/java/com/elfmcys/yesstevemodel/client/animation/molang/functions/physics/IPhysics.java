package com.elfmcys.yesstevemodel.client.animation.molang.functions.physics;

public interface IPhysics {
    void update(double timeStep);

    void setArgs(float... args);

    double getValue();
}
