/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.elfmcys.yesstevemodel.geckolib3.core.keyframe.event;

// Native Access
public class EventKeyFrame<T> {
    private final T eventData;
    private final float startTick;

    // Native Access
    public EventKeyFrame(double startTick, T eventData) {
        this.startTick = (float) startTick;
        this.eventData = eventData;
    }

    public T getEventData() {
        return eventData;
    }

    public float getStartTick() {
        return startTick;
    }
}