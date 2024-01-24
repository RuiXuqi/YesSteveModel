/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */
package com.elfmcys.yesstevemodel.geckolib3.core.builder;

import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.BoneAnimation;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.event.EventKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.event.ParticleEventKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.util.List;

// Native Access
public class Animation {
    public final String animationName;
    public final double animationLength;
    public final ILoopType loop;
    public final List<BoneAnimation> boneAnimations;
    public final List<EventKeyFrame<String>> soundKeyFrames;
    public final List<ParticleEventKeyFrame> particleKeyFrames;
    public final List<EventKeyFrame<IValue[]>> customInstructionKeyframes;

    // Native Access
    public Animation(String animationName, double animationLength, ILoopType loop, BoneAnimation[] boneAnimations, EventKeyFrame<String>[] soundKeyFrames, ParticleEventKeyFrame[] particleKeyFrames, EventKeyFrame<IValue[]>[] customInstructionKeyframes) {
        this.animationName = animationName;
        this.animationLength = animationLength;
        this.loop = loop;
        this.boneAnimations = ReferenceArrayList.wrap(boneAnimations);
        this.soundKeyFrames = ReferenceArrayList.wrap(soundKeyFrames);
        this.particleKeyFrames = ReferenceArrayList.wrap(particleKeyFrames);
        this.customInstructionKeyframes = ReferenceArrayList.wrap(customInstructionKeyframes);
    }
}
