package com.elfmcys.ysm.client.model.data;

import com.elfmcys.ysm.client.sound.data.SoundData;
import com.elfmcys.ysm.geckolib3.core.molang.value.IValue;

import java.util.Map;
import java.util.Objects;

public record CommonAssetData(Map<String, SoundData> sounds, Map<String, IValue> userFunctions,
                              Map<String, Map<String, String>> languageFiles) {
    public CommonAssetData {
        Objects.requireNonNull(sounds, "sounds");
        Objects.requireNonNull(userFunctions, "userFunctions");
        Objects.requireNonNull(languageFiles, "languageFiles");
    }
}
