package com.elfmcys.ysm.format.parser.pojo.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;

public class GeoModel {
    @SerializedName("format_version")
    public String formatVersion;
    @SerializedName("minecraft:geometry")
    public ArrayList<Geometry> minecraftGeometry;
}
