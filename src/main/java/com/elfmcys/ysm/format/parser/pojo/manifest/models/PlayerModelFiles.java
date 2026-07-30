package com.elfmcys.ysm.format.parser.pojo.manifest.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class PlayerModelFiles {
    public LinkedHashMap<String, String> model = new LinkedHashMap<>();
    public LinkedHashMap<String, String> animation = new LinkedHashMap<>();

    @SerializedName("animation_controllers")
    public List<String> animationControllers = new ArrayList<>();

    public List<PBRTextureSet> texture = new ArrayList<>();

    @Deprecated
    @SerializedName("sound_path")
    public String soundPath;
}
