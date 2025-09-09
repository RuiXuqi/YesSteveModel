package com.elfmcys.yesstevemodel.client.model;

import com.elfmcys.yesstevemodel.client.sound.SoundData;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

import java.util.List;
import java.util.Map;

public class CommonAsset {
    private final Map<String, SoundData> sounds;
    private final Int2ReferenceOpenHashMap<IValue> userFunctions;
    private final Int2ReferenceOpenHashMap<List<IValue>> eventHandlers;
    private final Map<String, Map<String, String>> languageFiles;

    public CommonAsset(Map<String, SoundData> sounds, Int2ReferenceOpenHashMap<IValue> userFunctions, Int2ReferenceOpenHashMap<List<IValue>> eventHandlers, Map<String, Map<String, String>> languageFiles) {
        this.sounds = sounds;
        this.userFunctions = userFunctions;
        this.eventHandlers = eventHandlers;
        this.languageFiles = languageFiles;
    }

    public Map<String, SoundData> sounds() {
        return sounds;
    }

    public Int2ReferenceOpenHashMap<IValue> userFunctions() {
        return userFunctions;
    }

    public Int2ReferenceOpenHashMap<List<IValue>> eventHandlers() {
        return eventHandlers;
    }

    public Map<String, Map<String, String>> languageFiles() {
        return languageFiles;
    }
}
