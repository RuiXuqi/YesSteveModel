package com.elfmcys.ysm.info;

import com.elfmcys.ysm.util.FifoHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceLists;

import java.util.List;

public class ModelMetadata {
    private final String name;
    private final String tips;
    private final ModelLicense license;
    private final List<ModelAuthor> authors;
    private final FifoHashMap<String, String> links;

    public ModelMetadata(String name, String tips, ModelLicense license, ModelAuthor[] authors, FifoHashMap<String, String> links) {
        this.name = name;
        this.tips = tips;
        this.license = license;
        this.authors = ReferenceLists.unmodifiable(ReferenceArrayList.wrap(authors));
        this.links = links;
    }

    public String name() {
        return name;
    }

    public String tips() {
        return tips;
    }

    public ModelLicense license() {
        return license;
    }

    public List<ModelAuthor> authors() {
        return authors;
    }

    public FifoHashMap<String, String> links() {
        return links;
    }
}
