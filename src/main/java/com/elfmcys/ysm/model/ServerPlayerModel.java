package com.elfmcys.ysm.model;

import it.unimi.dsi.fastutil.objects.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ServerPlayerModel {
    private final Map<String, Set<String>> animations;
    private final List<String> textures;

    public ServerPlayerModel(Map<String, String[]> animations, String[] textures) {
        this.animations = Object2ObjectMaps.unmodifiable(new Object2ObjectOpenHashMap<>(animations.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> ObjectSets.unmodifiable(ObjectOpenHashSet.of(entry.getValue()))))));
        this.textures = ObjectLists.unmodifiable(ObjectArrayList.of(textures));
    }

    public Map<String, Set<String>> animations() {
        return animations;
    }

    public List<String> textures() {
        return textures;
    }
}
