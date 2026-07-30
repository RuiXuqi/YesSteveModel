package com.elfmcys.ysm.format.parser.pojo.controller;

import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashMap;

public class AnimationController {
    @SerializedName("initial_state")
    public String initialState = "default";

    public LinkedHashMap<String, State> states = new LinkedHashMap<>();
}
