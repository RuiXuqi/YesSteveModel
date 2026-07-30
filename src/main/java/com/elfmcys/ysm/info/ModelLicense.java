package com.elfmcys.ysm.info;

// Native Access
public class ModelLicense {
    private final String type;
    private final String desc;

    // Native Access
    public ModelLicense(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public String type() {
        return type;
    }

    public String desc() {
        return desc;
    }
}
