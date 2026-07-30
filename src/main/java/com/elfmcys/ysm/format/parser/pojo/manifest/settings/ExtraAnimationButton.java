package com.elfmcys.ysm.format.parser.pojo.manifest.settings;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ExtraAnimationButton {
    public String id = "";
    public String name = "";
    public String sound = "";

    @SerializedName("config_forms")
    public List<ConfigForms> configForms = new ArrayList<>();
}
