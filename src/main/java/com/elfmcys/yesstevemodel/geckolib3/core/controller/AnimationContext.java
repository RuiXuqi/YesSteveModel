package com.elfmcys.yesstevemodel.geckolib3.core.controller;

public class AnimationContext {
    private float animTime;
    private boolean allAnimationsFinished;
    private boolean anyAnimationFinished;

    public void setAnimTime(float animTime) {
        this.animTime = animTime;
    }

    public void setAllAnimationsFinished(boolean allAnimationsFinished) {
        this.allAnimationsFinished = allAnimationsFinished;
    }

    public void setAnyAnimationFinished(boolean anyAnimationFinished) {
        this.anyAnimationFinished = anyAnimationFinished;
    }

    public float animTime() {
        return animTime;
    }

    public boolean isAllAnimationsFinished() {
        return allAnimationsFinished;
    }

    public boolean isAnyAnimationFinished() {
        return anyAnimationFinished;
    }
}
