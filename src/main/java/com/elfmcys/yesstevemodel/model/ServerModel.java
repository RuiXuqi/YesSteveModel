package com.elfmcys.yesstevemodel.model;

import com.elfmcys.yesstevemodel.info.ModelInfo;
import it.unimi.dsi.fastutil.objects.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Native Access
public class ServerModel {
    private final String name;
    private final Set<String> geoModels;
    private final Map<String, Set<String>> animations;
    private final List<String> textures;
    private final ModelInfo info;
    private final boolean isDefault;
    private final boolean isNeedAuth;

    // Native Access
    public ServerModel(String name, String[] geoModels, Map<String, String[]> animations, String[] textures, ModelInfo info, boolean isDefault, boolean isNeedAuth) {
        this.name = name;
        this.geoModels = ObjectSets.unmodifiable(ObjectOpenHashSet.of(geoModels));
        this.animations = Object2ObjectMaps.unmodifiable(new Object2ObjectOpenHashMap<>(animations.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> ObjectSets.unmodifiable(ObjectOpenHashSet.of(entry.getValue()))))));
        this.textures = ObjectLists.unmodifiable(ObjectArrayList.of(textures));
        this.info = info;
        this.isDefault = isDefault;
        this.isNeedAuth = isNeedAuth;
    }

    public String name() {
        return name;
    }

    public Set<String> geoModels() {
        return geoModels;
    }

    public Map<String, Set<String>> animations() {
        return animations;
    }

    public List<String> textures() {
        return textures;
    }

    public ModelInfo info() {
        return info;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isNeedAuth() {
        return isNeedAuth;
    }
}
