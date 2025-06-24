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
import org.jetbrains.annotations.Nullable;

import java.util.List;

// Native Access
public class Animation {
    public final String animationName;
    public final float animationLength;
    public final ILoopType loop;
    @Nullable
    public final IValue startDelay;
    @Nullable
    public final IValue loopDelay;
    @Nullable
    public final IValue blendWeight;
    @Nullable
    public final Boolean overridePreviousAnimation;
    public final List<BoneAnimation> boneAnimations;
    public final List<EventKeyFrame<String>> soundKeyFrames;
    public final List<ParticleEventKeyFrame> particleKeyFrames;
    public final List<EventKeyFrame<IValue[]>> customInstructionKeyframes;

    // Native Access
    public Animation(String animationName, double animationLength, ILoopType loop, @Nullable IValue startDelay, @Nullable IValue loopDelay, @Nullable IValue blendWeight, @Nullable Boolean overridePreviousAnimation, BoneAnimation[] boneAnimations, EventKeyFrame<String>[] soundKeyFrames, ParticleEventKeyFrame[] particleKeyFrames, EventKeyFrame<IValue[]>[] customInstructionKeyframes) {
        this.animationName = animationName;
        this.animationLength = (float) animationLength;
        this.loop = loop;
        this.startDelay = startDelay;
        this.loopDelay = loopDelay;
        this.blendWeight = blendWeight;
        this.overridePreviousAnimation = overridePreviousAnimation;
        this.boneAnimations = ReferenceArrayList.wrap(boneAnimations);
        this.soundKeyFrames = ReferenceArrayList.wrap(soundKeyFrames);
        this.particleKeyFrames = ReferenceArrayList.wrap(particleKeyFrames);
        this.customInstructionKeyframes = ReferenceArrayList.wrap(customInstructionKeyframes);
    }
}
