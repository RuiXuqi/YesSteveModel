package com.elfmcys.yesstevemodel.geckolib3.core.snapshot;

import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.PointData;

/**
 * 同一个 AnimationProcessor 内每个 IBone 的 BoneTopLevelSnapshot 是唯一的
 */
public class BoneTopLevelSnapshot extends BoneSnapshot {
    public final IBone bone;
    public final PointData cachedPointData = new PointData(); // 并行动画控制器需要缓存旋转参数

    public float mostRecentResetRotationTick = 0;
    public float mostRecentResetPositionTick = 0;
    public float mostRecentResetScaleTick = 0;
    public boolean isCurrentlyRunningRotationAnimation = true;
    public boolean isCurrentlyRunningPositionAnimation = true;
    public boolean isCurrentlyRunningScaleAnimation = true;

    public BoneTopLevelSnapshot(IBone bone) {
        super(bone.getName());
        rotationValueX = bone.getRotationX();
        rotationValueY = bone.getRotationY();
        rotationValueZ = bone.getRotationZ();

        positionOffsetX = bone.getPositionX();
        positionOffsetY = bone.getPositionY();
        positionOffsetZ = bone.getPositionZ();

        scaleValueX = bone.getScaleX();
        scaleValueY = bone.getScaleY();
        scaleValueZ = bone.getScaleZ();

        hidden = bone.isHidden();
        childrenHidden = bone.areChildrenHidden();

        this.bone = bone;
    }

    public BoneTopLevelSnapshot(IBone bone, boolean dontSaveRotations) {
        this(bone);
        if (dontSaveRotations) {
            rotationValueX = 0;
            rotationValueY = 0;
            rotationValueZ = 0;
        }
    }

    public void commit() {
        bone.setHidden(hidden, childrenHidden);

        bone.setRotationX(rotationValueX);
        bone.setRotationY(rotationValueY);
        bone.setRotationZ(rotationValueZ);

        bone.setPositionX(positionOffsetX);
        bone.setPositionY(positionOffsetY);
        bone.setPositionZ(positionOffsetZ);

        bone.setScaleX(scaleValueX);
        bone.setScaleY(scaleValueY);
        bone.setScaleZ(scaleValueZ);

        cachedPointData.rotationValueX = 0;
        cachedPointData.rotationValueY = 0;
        cachedPointData.rotationValueZ = 0;
    }
}
