/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.elfmcys.yesstevemodel.geckolib3.core.keyframe;

import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.BoneKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneTopLevelSnapshot;

import com.elfmcys.yesstevemodel.geckolib3.util.OrderedSegmentSearcher;
import org.jetbrains.annotations.Nullable;

public class BoneAnimationQueue {
    public final BoneTopLevelSnapshot topLevelSnapshot;
    public final BoneSnapshot transitionOffset;

    @Nullable 
    public OrderedSegmentSearcher<BoneKeyFrame> rotationKeyFrames;
    @Nullable
    public OrderedSegmentSearcher<BoneKeyFrame> positionKeyFrames;
    @Nullable
    public OrderedSegmentSearcher<BoneKeyFrame> scaleKeyFrames;

    private boolean active = false;
    private float blendWeight = 1;

    public AnimationPoint rotation;
    public AnimationPoint position;
    public AnimationPoint scale;

    public BoneAnimationQueue(BoneTopLevelSnapshot snapshot) {
        topLevelSnapshot = snapshot;
        transitionOffset = new BoneSnapshot(snapshot.bone);
    }

    public void setBoneAnimation(BoneAnimation animation) {
        if (!animation.rotationKeyFrames.isEmpty()) {
            rotationKeyFrames = new OrderedSegmentSearcher<>(animation.rotationKeyFrames, 0, BoneKeyFrame::getEndTick);
        } else {
            rotationKeyFrames = null;
        }
        if (!animation.positionKeyFrames.isEmpty()) {
            positionKeyFrames = new OrderedSegmentSearcher<>(animation.positionKeyFrames, 0, BoneKeyFrame::getEndTick);
        } else {
            positionKeyFrames = null;
        }
        if (!animation.scaleKeyFrames.isEmpty()) {
            scaleKeyFrames = new OrderedSegmentSearcher<>(animation.scaleKeyFrames, 0, BoneKeyFrame::getEndTick);
        } else {
            scaleKeyFrames = null;
        }
    }

    public BoneSnapshot transitionOffset() {
        return transitionOffset;
    }

    public AnimationPoint rotation() {
        return rotation;
    }

    public AnimationPoint position() {
        return position;
    }

    public AnimationPoint scale() {
        return scale;
    }

    public void updateTransitionOffset() {
        transitionOffset.copyFrom(topLevelSnapshot.bone);
    }

    /**
     * 该骨骼上是否有动画
     */
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * 权重不小于 0
     */
    public float getBlendWeight() {
        return blendWeight;
    }

    public void setBlendWeight(float blendWeight) {
        this.blendWeight = blendWeight > 0 ? blendWeight : 0;   // bb 里就是这样的
    }

    // 此处链表一般只含一个元素
    public void resetQueues() {
        rotation = null;
        position = null;
        scale = null;
    }
}