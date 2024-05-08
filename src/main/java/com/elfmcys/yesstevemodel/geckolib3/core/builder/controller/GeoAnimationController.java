package com.elfmcys.yesstevemodel.geckolib3.core.builder.controller;

import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.util.Map;

// Native Access
public class GeoAnimationController {
    private final String initialState;
    private final Map<String, GeoAnimationControllerState> states;

    // Native Access
    public GeoAnimationController(String initialState, Map<String, GeoAnimationControllerState> states) {
        this.initialState = initialState;
        this.states = Object2ReferenceMaps.unmodifiable(new Object2ReferenceOpenHashMap<>(states));
    }

    public String initialState() {
        return initialState;
    }

    public Map<String, GeoAnimationControllerState> states() {
        return states;
    }
}
