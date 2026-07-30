package com.elfmcys.ysm.format.parser.pojo.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class ExtraInfo {
    public String name = "";
    public String tips = "";

    @SerializedName("extra_animation_names")
    public List<String> extraAnimationNames = new ArrayList<>();

    public List<String> authors = new ArrayList<>();
    public String license = "All Rights Reserved";
    public boolean free;
}
