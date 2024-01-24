/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.elfmcys.yesstevemodel.geckolib3.core.keyframe.event;

// Native Access
public class EventKeyFrame<T> {
    private final T eventData;
    private final double startTick;

    // Native Access
    public EventKeyFrame(double startTick, T eventData) {
        this.startTick = startTick;
        this.eventData = eventData;
    }

    public T getEventData() {
        return eventData;
    }

    public double getStartTick() {
        return startTick;
    }
}