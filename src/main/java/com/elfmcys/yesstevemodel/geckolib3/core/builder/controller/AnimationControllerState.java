package com.elfmcys.yesstevemodel.geckolib3.core.builder.controller;

import com.elfmcys.yesstevemodel.geckolib3.core.controller.transition.IBlendTransition;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceLists;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// Native Access
public class AnimationControllerState {
    private final List<Pair<String, @Nullable IValue>> animations;
    private final List<Pair<String, IValue>> transitions;
    private final List<String> soundEffects;
    private final List<IValue> onEntry;
    private final List<IValue> onExit;
    private final IBlendTransition blendTransition;
    private final boolean blendViaShortestPath;

    // Native Access
    public AnimationControllerState(Pair<String, IValue>[] animations, Pair<String, IValue>[] transitions, String[] soundEffects, IValue[] onEntry, IValue[] onExit, IBlendTransition blendTransition, boolean blendViaShortestPath) {
        this.animations = ReferenceLists.unmodifiable(ReferenceArrayList.wrap(animations));
        this.transitions = ReferenceLists.unmodifiable(ReferenceArrayList.wrap(transitions));
        this.soundEffects = ReferenceLists.unmodifiable(ReferenceArrayList.wrap(soundEffects));
        this.onEntry = ReferenceLists.unmodifiable(ReferenceArrayList.wrap(onEntry));
        this.onExit = ReferenceLists.unmodifiable(ReferenceArrayList.wrap(onExit));
        this.blendTransition = blendTransition;
        this.blendViaShortestPath = blendViaShortestPath;
    }

    public List<Pair<String, @Nullable IValue>> animations() {
        return animations;
    }

    public List<Pair<String, IValue>> transitions() {
        return transitions;
    }

    public List<String> soundEffects() {
        return soundEffects;
    }

    public List<IValue> onEntry() {
        return onEntry;
    }

    public List<IValue> onExit() {
        return onExit;
    }

    public IBlendTransition blendTransition() {
        return blendTransition;
    }

    public boolean blendViaShortestPath() {
        return blendViaShortestPath;
    }
}
