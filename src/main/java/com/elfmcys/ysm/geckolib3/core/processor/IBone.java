package com.elfmcys.ysm.geckolib3.core.processor;

import org.joml.Vector3f;

public interface IBone {
    float getRotationX();

    void setRotationX(float value);

    float getRotationY();

    void setRotationY(float value);

    float getRotationZ();

    void setRotationZ(float value);

    float getPositionX();

    void setPositionX(float value);

    float getPositionY();

    void setPositionY(float value);

    float getPositionZ();

    void setPositionZ(float value);

    float getScaleX();

    void setScaleX(float value);

    float getScaleY();

    void setScaleY(float value);

    float getScaleZ();

    void setScaleZ(float value);

    float getPivotX();

    float getPivotY();

    float getPivotZ();

    boolean isHidden();

    void setHidden(boolean hidden);

    boolean areChildrenHidden();

    void setHidden(boolean selfHidden, boolean skipChildRendering);

    boolean isTracking();

    void setTracking(boolean tracking);

    Vector3f getInitialRotation();

    float getAbsolutePivotX();
    
    float getAbsolutePivotY();

    float getAbsolutePivotZ();

    String getName();

    int getPooledName();
}
