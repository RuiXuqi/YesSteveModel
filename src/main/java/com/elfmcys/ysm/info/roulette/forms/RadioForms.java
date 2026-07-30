package com.elfmcys.ysm.info.roulette.forms;

import com.elfmcys.ysm.util.FifoHashMap;

public class RadioForms extends ConfigForms {
    public static final String TYPE = "radio";

    private final FifoHashMap<String, String> labels;

    public RadioForms(String title, String description, String value, FifoHashMap<String, String> labels) {
        super(TYPE, title, description, value);
        this.labels = labels;
    }

    public FifoHashMap<String, String> labels() {
        return labels;
    }
}
