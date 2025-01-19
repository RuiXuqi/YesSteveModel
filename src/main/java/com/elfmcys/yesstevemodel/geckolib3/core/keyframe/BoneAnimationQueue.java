/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.elfmcys.yesstevemodel.geckolib3.core.keyframe;

import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneTopLevelSnapshot;

import org.jetbrains.annotations.Nullable;

public class BoneAnimationQueue {
    public final BoneTopLevelSnapshot topLevelSnapshot;
    public final BoneSnapshot controllerSnapshot;
    @Nullable 
    public BoneAnimation animation;
    private boolean active = false;

    public final AnimationPointQueue rotationQueue = new AnimationPointQueue();
    public final AnimationPointQueue positionQueue = new AnimationPointQueue();
    public final AnimationPointQueue scaleQueue = new AnimationPointQueue();

    public BoneAnimationQueue(BoneTopLevelSnapshot snapshot) {
        topLevelSnapshot = snapshot;
        controllerSnapshot = new BoneSnapshot(snapshot);
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
        controllerSnapshot.copyFrom(topLevelSnapshot);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // 此处链表一般只含一个元素
    public void resetQueues() {
        rotationQueue.clear();
        positionQueue.clear();
        scaleQueue.clear();
    }
}