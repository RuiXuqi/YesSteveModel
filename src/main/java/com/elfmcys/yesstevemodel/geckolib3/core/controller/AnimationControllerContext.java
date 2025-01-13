package com.elfmcys.yesstevemodel.geckolib3.core.controller;

public class AnimationControllerContext {
    private double animTime;
    /**
     * 如果当前动画已播放至少一次，则返回 true
     */
    private boolean animIsFinished;

    public void setAnimTime(double animTime) {
        this.animTime = animTime;
    }

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
