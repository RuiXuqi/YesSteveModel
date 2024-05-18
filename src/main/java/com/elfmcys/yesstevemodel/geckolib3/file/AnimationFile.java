package com.elfmcys.yesstevemodel.geckolib3.file;

import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.util.Map;

// Native Access
public class AnimationFile {
    private final Map<String, Animation> animations;

    // Native Access
    public AnimationFile(Map<String, Animation> animations) {
        this.animations = Object2ReferenceMaps.unmodifiable(new Object2ReferenceOpenHashMap<>(animations));
    }

    public Map<String, Animation> animations() {
        return this.animations;
    }
}