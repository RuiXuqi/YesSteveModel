/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.elfmcys.yesstevemodel.geckolib3.core.snapshot;

import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;

public class BoneSnapshot {
    public final String name;
    public float scaleValueX;
    public float scaleValueY;
    public float scaleValueZ;
    public float positionOffsetX;
    public float positionOffsetY;
    public float positionOffsetZ;
    public float rotationValueX;
    public float rotationValueY;
    public float rotationValueZ;

    public boolean hidden;
    public boolean childrenHidden;

    public BoneSnapshot(IBone bone) {
        copyFrom(bone);
        this.name = bone.getName();
    }

    public void copyFrom(IBone bone) {
        scaleValueX = bone.getScaleX();
        scaleValueY = bone.getScaleY();
        scaleValueZ = bone.getScaleZ();

        positionOffsetX = bone.getPositionX();
        positionOffsetY = bone.getPositionY();
        positionOffsetZ = bone.getPositionZ();

        rotationValueX = bone.getRotationX();
        rotationValueY = bone.getRotationY();
        rotationValueZ = bone.getRotationZ();

        hidden = bone.isHidden();
        childrenHidden = bone.areChildrenHidden();
    }

    public void copyFrom(BoneSnapshot snapshot) {
        scaleValueX = snapshot.scaleValueX;
        scaleValueY = snapshot.scaleValueY;
        scaleValueZ = snapshot.scaleValueZ;

        positionOffsetX = snapshot.positionOffsetX;
        positionOffsetY = snapshot.positionOffsetY;
        positionOffsetZ = snapshot.positionOffsetZ;

        rotationValueX = snapshot.rotationValueX;
        rotationValueY = snapshot.rotationValueY;
        rotationValueZ = snapshot.rotationValueZ;

        hidden = snapshot.hidden;
        childrenHidden = snapshot.childrenHidden;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if(o instanceof BoneSnapshot) {
            BoneSnapshot that = (BoneSnapshot) o;
            return name.equals(that.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
