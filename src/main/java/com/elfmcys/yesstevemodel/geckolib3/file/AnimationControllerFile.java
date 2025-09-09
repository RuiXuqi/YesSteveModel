package com.elfmcys.yesstevemodel.geckolib3.file;

import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.AnimationControllerData;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.util.Map;

// Native Access
public class AnimationControllerFile {
    private final Map<String, AnimationControllerData> animationControllers;

    // Native Access
    public AnimationControllerFile(Map<String, AnimationControllerData> animationControllers) {
        this.animationControllers = Object2ReferenceMaps.unmodifiable(new Object2ReferenceOpenHashMap<>(animationControllers));
    }

    public Map<String, AnimationControllerData> animationControllers() {
        return this.animationControllers;
    }
}