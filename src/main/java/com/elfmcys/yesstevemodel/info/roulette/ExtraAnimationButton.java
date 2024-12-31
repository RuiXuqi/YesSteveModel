package com.elfmcys.yesstevemodel.info.roulette;

import com.elfmcys.yesstevemodel.info.roulette.forms.ConfigForms;

// Native Access
public class ExtraAnimationButton {
    private final String id;
    private final String name;
    private final String sound;
    private final ConfigForms[] configForms;

    // Native Access
    public ExtraAnimationButton(String id, String name, String sound, ConfigForms[] configForms) {
        this.id = id;
        this.name = name;
        this.sound = sound;
        this.configForms = configForms;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSound() {
        return sound;
    }

    public ConfigForms[] getConfigForms() {
        return configForms;
    }
}
