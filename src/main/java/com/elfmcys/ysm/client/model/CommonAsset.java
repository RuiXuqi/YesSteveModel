package com.elfmcys.ysm.client.model;

import com.elfmcys.ysm.client.sound.data.SoundData;
import com.elfmcys.ysm.geckolib3.core.molang.value.IValue;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.util.List;
import java.util.Map;

public class CommonAsset {
    private final Map<String, SoundData> sounds;
    private final Object2ReferenceOpenHashMap<String, IValue> userFunctions;
    private final Object2ReferenceOpenHashMap<String, List<IValue>> eventHandlers;
    private final Map<String, Map<String, String>> languageFiles;

    public CommonAsset(Map<String, SoundData> sounds, Object2ReferenceOpenHashMap<String, IValue> userFunctions, Object2ReferenceOpenHashMap<String, List<IValue>> eventHandlers, Map<String, Map<String, String>> languageFiles) {
        this.sounds = sounds;
        this.userFunctions = userFunctions;
        this.eventHandlers = eventHandlers;
        this.languageFiles = languageFiles;
    }

    public Map<String, SoundData> sounds() {
        return sounds;
    }

    public Object2ReferenceOpenHashMap<String, IValue> userFunctions() {
        return userFunctions;
    }

    public Object2ReferenceOpenHashMap<String, List<IValue>> eventHandlers() {
        return eventHandlers;
    }

    public Map<String, Map<String, String>> languageFiles() {
        return languageFiles;
    }
}
