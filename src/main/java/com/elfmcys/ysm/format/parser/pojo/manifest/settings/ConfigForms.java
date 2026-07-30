package com.elfmcys.ysm.format.parser.pojo.manifest.settings;

import java.util.LinkedHashMap;

public class ConfigForms {
    public String type = "";
    public String title = "";
    public String description = "";
    public String value = "";
    public double step = 1;
    public double min = 0;
    public double max = 100;
    public LinkedHashMap<String, String> labels = new LinkedHashMap<>();
}
