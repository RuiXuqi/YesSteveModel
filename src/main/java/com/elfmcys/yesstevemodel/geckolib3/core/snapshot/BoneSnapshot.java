/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.elfmcys.yesstevemodel.geckolib3.core.snapshot;

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

    protected BoneSnapshot(String name) {
        this.name = name;
    }

    public BoneSnapshot(BoneSnapshot snapshot) {
        copyFrom(snapshot);
        this.name = snapshot.name;
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
