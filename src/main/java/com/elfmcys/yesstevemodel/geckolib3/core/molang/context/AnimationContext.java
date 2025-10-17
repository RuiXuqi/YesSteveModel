package com.elfmcys.yesstevemodel.geckolib3.core.molang.context;

import com.elfmcys.yesstevemodel.client.sound.instance.SoundInstanceManager;

public class AnimationContext {
    private SoundInstanceManager soundManager;
    private float animTime;

    public void setAnimTime(float animTime) {
        this.animTime = animTime;
    }

    public float animTime() {
        return animTime;
    }

    public SoundInstanceManager soundManager() {
        if (soundManager == null) {
            soundManager = new SoundInstanceManager();
        }
        return soundManager;
    }
}
