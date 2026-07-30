package com.elfmcys.ysm.format.parser.pojo.manifest.metadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ModelMetadata {
    public String name = "";
    public String tips = "";
    public ModelLicense license = new ModelLicense();
    public List<ModelAuthor> authors = new ArrayList<>();
    public LinkedHashMap<String, String> link = new LinkedHashMap<>();
}
