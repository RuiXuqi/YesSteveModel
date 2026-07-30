package com.elfmcys.ysm.format.parser.pojo.manifest.settings;

import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashMap;

public class ExtraAnimationClassify {
    public String id = "";

    @SerializedName("extra_animation")
    public LinkedHashMap<String, String> extraAnimation = new LinkedHashMap<>();
}
