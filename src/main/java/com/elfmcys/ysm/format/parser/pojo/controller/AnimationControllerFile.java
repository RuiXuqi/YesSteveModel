package com.elfmcys.ysm.format.parser.pojo.controller;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class AnimationControllerFile {
    @SerializedName("animation_controllers")
    public Map<String, AnimationController> animationControllers = new HashMap<>();
}
