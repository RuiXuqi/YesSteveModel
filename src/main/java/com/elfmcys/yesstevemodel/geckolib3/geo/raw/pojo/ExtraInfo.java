package com.elfmcys.yesstevemodel.geckolib3.geo.raw.pojo;

// Native Access
public class ExtraInfo {
    private String name;
    private String tips;
    private String[] extraAnimationNames;
    private String[] authors;
    private String license;

    // Native Access
    public ExtraInfo(String name, String tips, String[] extraAnimationNames, String[] authors, String license) {
        this.name = name;
        this.tips = tips;
        this.extraAnimationNames = extraAnimationNames;
        this.authors = authors;
        this.license = license;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTips() {
        return tips;
    }

    public void setTips(String tips) {
        this.tips = tips;
    }

    public String[] getExtraAnimationNames() {
        return extraAnimationNames;
    }

    public void setExtraAnimationNames(String[] extraAnimationNames) {
        this.extraAnimationNames = extraAnimationNames;
    }

    public String[] getAuthors() {
        return authors;
    }

    public void setAuthors(String[] authors) {
        this.authors = authors;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }
}
