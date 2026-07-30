package com.elfmcys.ysm.format.parser.pojo.manifest.models;

import java.util.ArrayList;
import java.util.List;

public class ReplacedModelFiles {
    public List<String> match = new ArrayList<>();
    public String model = "";
    public String animation;
    public String controller;
    public PBRTextureSet texture = new PBRTextureSet();
}
