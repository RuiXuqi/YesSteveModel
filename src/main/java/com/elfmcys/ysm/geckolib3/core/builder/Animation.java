/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */
package com.elfmcys.ysm.geckolib3.core.builder;

import com.elfmcys.ysm.geckolib3.core.keyframe.BoneAnimation;
import com.elfmcys.ysm.geckolib3.core.keyframe.event.EventKeyFrame;
import com.elfmcys.ysm.geckolib3.core.keyframe.event.ParticleEventKeyFrame;
import com.elfmcys.ysm.geckolib3.core.molang.value.IValue;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// Native Access
public class Animation {
    public final String name;
    public final float animationLength;
    public final LoopType loop;
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

    // 是否是复制自默认模型的动画，用来纠正 2.4.1 及以前手部动画播放错误的问题
    public boolean isCopiedFromDefaultModel = false;

    // Native Access
    public Animation(String name, double animationLength, LoopType loop, @Nullable IValue startDelay, @Nullable IValue loopDelay, @Nullable IValue blendWeight, @Nullable Boolean overridePreviousAnimation, BoneAnimation[] boneAnimations, EventKeyFrame<String>[] soundKeyFrames, ParticleEventKeyFrame[] particleKeyFrames, EventKeyFrame<IValue[]>[] customInstructionKeyframes) {
        this.name = name;
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

    public boolean isEmpty() {
        return boneAnimations.isEmpty() && soundKeyFrames.isEmpty() && particleKeyFrames.isEmpty() && customInstructionKeyframes.isEmpty();
    }
}
