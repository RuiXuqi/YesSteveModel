package com.elfmcys.yesstevemodel.geckolib3.core.builder.controller;

import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.util.Map;

// Native Access
public class AnimationControllerData {
    private final String initialState;
    private final Map<String, AnimationControllerState> states;

    // Native Access
    public AnimationControllerData(String initialState, Map<String, AnimationControllerState> states) {
        this.initialState = initialState;
        this.states = Object2ReferenceMaps.unmodifiable(new Object2ReferenceOpenHashMap<>(states));
    }

    public String initialState() {
        return initialState;
    }

    public Map<String, AnimationControllerState> states() {
        return states;
    }
}
