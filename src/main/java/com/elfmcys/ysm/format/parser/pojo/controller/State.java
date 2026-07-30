package com.elfmcys.ysm.format.parser.pojo.controller;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class State {
    public List<AnimationEntry> animations = new ArrayList<>();
    public List<Transition> transitions = new ArrayList<>();

    @SerializedName("sound_effects")
    public List<SoundEffect> soundEffects = new ArrayList<>();

    @SerializedName("on_entry")
    public List<String> onEntry = new ArrayList<>();

    @SerializedName("on_exit")
    public List<String> onExit = new ArrayList<>();

    @SerializedName("blend_transition")
    public BlendTransition blendTransition = new BlendTransition();

    @SerializedName("blend_via_shortest_path")
    public boolean blendViaShortestPath = false;
}
