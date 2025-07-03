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
    public final BoneSnapshot controllerSnapshot;

    @Nullable 
    public OrderedSegmentSearcher<BoneKeyFrame> rotationKeyFrames;
    @Nullable
    public OrderedSegmentSearcher<BoneKeyFrame> positionKeyFrames;
    @Nullable
    public OrderedSegmentSearcher<BoneKeyFrame> scaleKeyFrames;

    private boolean active = false;
    private float blendWeight = 1;

    public final AnimationPointQueue rotationQueue = new AnimationPointQueue();
    public final AnimationPointQueue positionQueue = new AnimationPointQueue();
    public final AnimationPointQueue scaleQueue = new AnimationPointQueue();

    public BoneAnimationQueue(BoneTopLevelSnapshot snapshot) {
        topLevelSnapshot = snapshot;
        controllerSnapshot = new BoneSnapshot(snapshot.bone);
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

    public BoneSnapshot snapshot() {
        return controllerSnapshot;
    }

    public AnimationPointQueue rotationQueue() {
        return rotationQueue;
    }

    public AnimationPointQueue positionQueue() {
        return positionQueue;
    }

    public AnimationPointQueue scaleQueue() {
        return scaleQueue;
    }

    public void updateSnapshot() {
        controllerSnapshot.copyFrom(topLevelSnapshot.bone);
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

    public float getBlendWeight() {
        return blendWeight;
    }

    public void setBlendWeight(float blendWeight) {
        this.blendWeight = blendWeight;
    }

    // 此处链表一般只含一个元素
    public void resetQueues() {
        rotationQueue.clear();
        positionQueue.clear();
        scaleQueue.clear();
    }
}