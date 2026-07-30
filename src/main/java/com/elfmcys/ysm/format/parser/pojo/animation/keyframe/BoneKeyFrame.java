package com.elfmcys.ysm.format.parser.pojo.animation.keyframe;

import com.elfmcys.ysm.format.parser.pojo.animation.value.Vector3v;
import com.elfmcys.ysm.geckolib3.core.keyframe.bone.EasingType;

public class BoneKeyFrame {
    public float startTick = 0;
    public EasingType easingType = EasingType.LINEAR;
    public Vector3v preValue;
    public Vector3v postValue;
}
