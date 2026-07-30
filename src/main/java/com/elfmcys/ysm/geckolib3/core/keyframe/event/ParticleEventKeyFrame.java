package com.elfmcys.ysm.geckolib3.core.keyframe.event;

// Native Access
public class ParticleEventKeyFrame extends EventKeyFrame<String> {
    public final String effect;
    public final String locator;
    public final String script;

    // Native Access
    public ParticleEventKeyFrame(double startTick, String eventData, String effect, String locator, String script) {
        super(startTick, eventData);
        this.effect = effect;
        this.locator = locator;
        this.script = script;
    }
}