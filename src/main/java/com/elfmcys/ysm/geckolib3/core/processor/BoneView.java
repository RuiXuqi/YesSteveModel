package com.elfmcys.ysm.geckolib3.core.processor;

public interface BoneView {
    String getName();

    int getPooledName();

    float getPositionX();

    float getPositionY();

    float getPositionZ();

    float getInitialRotationX();

    float getInitialRotationY();

    float getInitialRotationZ();

    float getScaleX();

    float getScaleY();

    float getScaleZ();

    float getRotationX();

    float getRotationY();

    float getRotationZ();

    float getPivotX();

    float getPivotY();

    float getPivotZ();

    boolean areCubesHidden();

    boolean areChildrenHidden();
}
