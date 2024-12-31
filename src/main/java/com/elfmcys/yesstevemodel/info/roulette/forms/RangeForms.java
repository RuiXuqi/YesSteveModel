package com.elfmcys.yesstevemodel.info.roulette.forms;

// Native Access
public class RangeForms extends ConfigForms {
    public static final String TYPE = "range";

    private final double step;
    private final double min;
    private final double max;

    // Native Access
    public RangeForms(String title, String description, String value, double step, double min, double max) {
        super(TYPE, title, description, value);
        this.step = step;
        this.min = min;
        this.max = max;
    }

    public double step() {
        return step;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }
}
