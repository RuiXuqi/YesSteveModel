package com.elfmcys.yesstevemodel.geckolib3.core.controller;

public class AnimationContext {
    private double animTime;
    private boolean allAnimationsFinished;
    private boolean anyAnimationFinished;

    public void setAnimTime(double animTime) {
        this.animTime = animTime;
    }

    public void setAllAnimationsFinished(boolean allAnimationsFinished) {
        this.allAnimationsFinished = allAnimationsFinished;
    }

    public void setAnyAnimationFinished(boolean anyAnimationFinished) {
        this.anyAnimationFinished = anyAnimationFinished;
    }

    public double animTime() {
        return animTime;
    }

    public boolean isAllAnimationsFinished() {
        return allAnimationsFinished;
    }

    public boolean isAnyAnimationFinished() {
        return anyAnimationFinished;
    }
}
