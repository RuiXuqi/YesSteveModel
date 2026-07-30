package com.elfmcys.ysm.geckolib3.core.snapshot;

import com.elfmcys.ysm.geckolib3.core.processor.IBone;
import org.joml.Vector3f;

/**
 * 同一个 AnimationProcessor 内每个 IBone 的 BoneTopLevelSnapshot 是唯一的
 */
public class BoneTopLevelSnapshot extends BoneSnapshot {
    public final IBone bone;

    // 历史遗留问题，CodedAnimationController 的并行动画控制器需要缓存旋转参数
    @Deprecated
    public final Vector3f cachedPointData = new Vector3f();

    public boolean hasAnimation = false;
    public boolean isCurrentlyRunningRotationAnimation = true;
    public boolean isCurrentlyRunningPositionAnimation = true;
    public boolean isCurrentlyRunningScaleAnimation = true;

    public float lastRotationUpdateTime;
    public float lastPositionUpdateTime;
    public float lastScaleUpdateTime;

    public Vector3f rotationOffset;
    public Vector3f positionOffset;
    public Vector3f scaleOffset;

    public BoneTopLevelSnapshot(IBone bone) {
        super(bone);
        this.bone = bone;
    }

    public void commit() {
        bone.setHidden(hidden, childrenHidden);

        var initRot = bone.getInitialRotation();
        bone.setRotationX(rotation.x + initRot.x);
        bone.setRotationY(rotation.y + initRot.y);
        bone.setRotationZ(rotation.z + initRot.z);

        bone.setPositionX(position.x);
        bone.setPositionY(position.y);
        bone.setPositionZ(position.z);

        bone.setScaleX(scale.x);
        bone.setScaleY(scale.y);
        bone.setScaleZ(scale.z);

        cachedPointData.set(0, 0, 0);
    }
}
