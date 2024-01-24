package com.elfmcys.yesstevemodel.model;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class ServerModel {
    private final String name;
    private final Set<String> geoModels;
    private final Set<String> animationFiles;
    private final Set<String> textures;
    private final Set<String> features;
    // 模型哈希
    private final String hash;
    private final boolean isDefault;
    private final boolean isNeedAuth;

    public ServerModel(String name, String[] geoModels, String[] animationFiles, String[] textures, String[] features, String hash, boolean isDefault, boolean isNeedAuth) {
        this.name = name;
        this.geoModels = ImmutableSet.copyOf(geoModels);
        this.animationFiles = ImmutableSet.copyOf(animationFiles);
        this.textures = ImmutableSet.copyOf(textures);
        this.features = ImmutableSet.copyOf(features);
        this.hash = hash;
        this.isDefault = isDefault;
        this.isNeedAuth = isNeedAuth;
    }

    public String name() {
        return name;
    }

    public Set<String> geoModels() {
        return geoModels;
    }

    public Set<String> animationFiles() {
        return animationFiles;
    }

    public Set<String> textures() {
        return textures;
    }

    public Set<String> features() {
        return features;
    }

    public String hash() {
        return hash;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isNeedAuth() {
        return isNeedAuth;
    }
}
