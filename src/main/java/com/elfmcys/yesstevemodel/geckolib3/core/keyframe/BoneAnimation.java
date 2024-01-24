/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.elfmcys.yesstevemodel.geckolib3.core.keyframe;

import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.BoneKeyFrame;

import java.util.List;

// Native Access
public class BoneAnimation {
    public final String boneName;
    public final List<BoneKeyFrame> rotationKeyFrames;
    public final List<BoneKeyFrame> positionKeyFrames;
    public final List<BoneKeyFrame> scaleKeyFrames;

    // Native Access
    public BoneAnimation(String boneName, List<BoneKeyFrame> rotationKeyFrames, List<BoneKeyFrame> positionKeyFrames, List<BoneKeyFrame> scaleKeyFrames) {
        this.boneName = boneName;
        this.rotationKeyFrames = rotationKeyFrames;
        this.positionKeyFrames = positionKeyFrames;
        this.scaleKeyFrames = scaleKeyFrames;
    }
}