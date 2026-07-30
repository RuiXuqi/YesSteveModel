package com.elfmcys.ysm.format.parser.pojo.animation;

import com.elfmcys.ysm.format.parser.pojo.animation.keyframe.BoneKeyFrameList;
import com.google.gson.annotations.SerializedName;

public class BoneAnimation {
    public String boneName = "";

    @SerializedName("rotation")
    public BoneKeyFrameList rotationKeyFrames = new BoneKeyFrameList();

    @SerializedName("position")
    public BoneKeyFrameList positionKeyFrames = new BoneKeyFrameList();

    @SerializedName("scale")
    public BoneKeyFrameList scaleKeyFrames = new BoneKeyFrameList();
}
