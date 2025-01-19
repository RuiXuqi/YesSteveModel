package com.elfmcys.yesstevemodel.geckolib3.core.controller;

public class AnimationContext {
    private double animTime;
    /**
     * 如果当前动画已播放至少一次，则返回 true
     */
    private boolean animIsFinished;

    public void setAnimTime(double animTime) {
        this.animTime = animTime;
    }

    // FIXME: 完成混合动画支持
    public void setAnimIsFinished(boolean animIsFinished) {
        this.animIsFinished = animIsFinished;
    }

    public double animTime() {
        return animTime;
    }

    public boolean animIsFinished() {
        return animIsFinished;
    }
}
