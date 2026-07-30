/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.elfmcys.ysm.geckolib3.core.snapshot;

import com.elfmcys.ysm.geckolib3.core.processor.IBone;
import org.joml.Vector3f;

public class BoneSnapshot {
    public final int name;

    public final Vector3f position = new Vector3f();
    public final Vector3f rotation = new Vector3f();
    public final Vector3f scale = new Vector3f(1, 1, 1);

    public boolean hidden;
    public boolean childrenHidden;

    public BoneSnapshot(IBone bone) {
        copyFrom(bone);
        this.name = bone.getPooledName();
    }

    public void copyFrom(IBone bone) {
        var initRot = bone.getInitialRotation();

        position.set(bone.getPositionX(), bone.getPositionY(), bone.getPositionZ());
        rotation.set(bone.getRotationX() - initRot.x, bone.getRotationY() - initRot.y, bone.getRotationZ() - initRot.z);
        scale.set(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());

        hidden = bone.isHidden();
        childrenHidden = bone.areChildrenHidden();
    }

    public void copyFrom(BoneSnapshot snapshot) {
        position.set(snapshot.position);
        rotation.set(snapshot.rotation);
        scale.set(snapshot.scale);

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
            return name == that.name;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name;
    }
}
