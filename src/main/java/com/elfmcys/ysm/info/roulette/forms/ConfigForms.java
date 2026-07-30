package com.elfmcys.ysm.info.roulette.forms;

// Native Access
public abstract class ConfigForms {
    private final String type;
    private final String title;
    private final String description;
    private final String value;

    // Native Access
    public ConfigForms(String type, String title, String description, String value) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.value = value;
    }

    public String type() {
        return type;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String value() {
        return value;
    }
}
