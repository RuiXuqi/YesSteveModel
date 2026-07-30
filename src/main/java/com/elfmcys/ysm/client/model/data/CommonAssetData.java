package com.elfmcys.ysm.client.model.data;

import com.elfmcys.ysm.client.sound.data.SoundData;
import com.elfmcys.ysm.geckolib3.core.molang.value.IValue;

import java.util.Map;

// Native Access
public class CommonAssetData {
    private final Map<String, SoundData> sounds;
    private final Map<String, IValue> userFunctions;
    private final Map<String, Map<String, String>> languageFiles;

    // Native Access
    public CommonAssetData(Map<String, SoundData> sounds, Map<String, IValue> userFunctions, Map<String, Map<String, String>> languageFiles) {
        this.sounds = sounds;
        this.userFunctions = userFunctions;
        this.languageFiles = languageFiles;
    }

    public Map<String, SoundData> sounds() {
        return sounds;
    }

    public Map<String, IValue> userFunctions() {
        return userFunctions;
    }

    public Map<String, Map<String, String>> languageFiles() {
        return languageFiles;
    }
}
