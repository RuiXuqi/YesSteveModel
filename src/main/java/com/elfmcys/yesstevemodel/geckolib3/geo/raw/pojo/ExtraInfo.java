package com.elfmcys.yesstevemodel.geckolib3.geo.raw.pojo;

// Native Access
public class ExtraInfo {
    private final String name;
    private final String tips;
    private final String[] extraAnimationNames;
    private final String[] authors;
    private final String license;
    private final boolean free;

    // Native Access
    public ExtraInfo(String name, String tips, String[] extraAnimationNames, String[] authors, String license, boolean free) {
        this.name = name;
        this.tips = tips;
        this.extraAnimationNames = extraAnimationNames;
        this.authors = authors;
        this.license = license;
        this.free = free;
    }

    public String getName() {
        return name;
    }

    public String getTips() {
        return tips;
    }

    public String[] getExtraAnimationNames() {
        return extraAnimationNames;
    }

    public String[] getAuthors() {
        return authors;
    }

    public String getLicense() {
        return license;
    }

    public boolean isFree() {
        return this.free;
    }
}
