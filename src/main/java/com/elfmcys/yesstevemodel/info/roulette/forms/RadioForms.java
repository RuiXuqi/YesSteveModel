package com.elfmcys.yesstevemodel.info.roulette.forms;

import com.elfmcys.yesstevemodel.util.FifoHashMap;

// Native Access
public class RadioForms extends ConfigForms {
    public static final String TYPE = "radio";

    private final FifoHashMap<String, String> labels;

    // Native Access
    public RadioForms(String title, String description, String value, FifoHashMap<String, String> labels) {
        super(TYPE, title, description, value);
        this.labels = labels;
    }

    public FifoHashMap<String, String> labels() {
        return labels;
    }
}
