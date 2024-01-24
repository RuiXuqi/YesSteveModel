package com.elfmcys.yesstevemodel.geckolib3.file;

import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.util.Collection;
import java.util.Map;

// Native Access
public class AnimationFile {
    private final Map<String, Animation> animations;

    // Native Access
    public AnimationFile(Map<String, Animation> animations) {
        this.animations = new Object2ReferenceOpenHashMap<>(animations);
    }

    public AnimationFile() {
        this(new Object2ReferenceOpenHashMap<>());
    }

    public Animation getAnimation(String name) {
        return animations.get(name);
    }

    public Collection<Animation> getAllAnimations() {
        return this.animations.values();
    }

    public Map<String, Animation> getAnimations() {
        return this.animations;
    }

    public void putAnimation(String name, Animation animation) {
        this.animations.put(name, animation);
    }
}