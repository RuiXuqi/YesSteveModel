/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.elfmcys.yesstevemodel.geckolib3.core;

public enum AnimationState {
    /**
     * 空闲中
     */
    IDLE,
    /**
     * 过渡中
     */
    TRANSITIONING,
    /**
     * 播放中
     */
    RUNNING,
    /**
     * 被外部暂停
     */
    STOPPING
}