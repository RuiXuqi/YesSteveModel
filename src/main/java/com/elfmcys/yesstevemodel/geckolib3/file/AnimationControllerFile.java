package com.elfmcys.yesstevemodel.geckolib3.file;

import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationController;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.util.Map;

// Native Access
public class AnimationControllerFile {
    private final Map<String, GeoAnimationController> animationControllers;

    // Native Access
    public AnimationControllerFile(Map<String, GeoAnimationController> animationControllers) {
        this.animationControllers = Object2ReferenceMaps.unmodifiable(new Object2ReferenceOpenHashMap<>(animationControllers));
    }

    public Map<String, GeoAnimationController> animationControllers() {
        return this.animationControllers;
    }
}