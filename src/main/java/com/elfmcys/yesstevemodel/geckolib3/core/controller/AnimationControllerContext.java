package com.elfmcys.yesstevemodel.geckolib3.core.controller;

public class AnimationControllerContext {
    private double lifeTime;
    private double animTime;

    public void setLifeTime(double lifeTime) {
        this.lifeTime = lifeTime;
    }

    public void setAnimTime(double animTime) {
        this.animTime = animTime;
    }

    public double lifeTime() {
        return lifeTime;
    }

    public double animTime() {
        return animTime;
    }
}
