package com.elfmcys.yesstevemodel.client.data;

import com.elfmcys.yesstevemodel.client.texture.NativeTexture;
import com.elfmcys.yesstevemodel.geckolib3.file.AnimationFile;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;

// Native Access
public final class ClientModel {
    private final String name;
    private final Map<String, GeoModel> geoModels;
    private final Map<String, AnimationFile> animationFiles;
    private final Map<String, NativeTexture> textures;
    private final Set<String> features;
    private final String hash;
    private final boolean isDefault;
    private final boolean isNeedAuth;

    // Native Access
    public ClientModel(String name, Map<String, GeoModel> geoModels, Map<String, AnimationFile> animationFiles, Map<String, NativeTexture> textures, String[] features, String hash, boolean isDefault, boolean isNeedAuth) {
        this.name = name;
        this.geoModels = ImmutableMap.copyOf(geoModels);
        this.animationFiles = ImmutableMap.copyOf(animationFiles);
        this.textures = ImmutableMap.copyOf(textures);
        this.features = ImmutableSet.copyOf(features);
        this.hash = hash;
        this.isDefault = isDefault;
        this.isNeedAuth = isNeedAuth;
    }

    public String name() {
        return name;
    }

    public Map<String, GeoModel> geoModels() {
        return geoModels;
    }

    public Map<String, AnimationFile> animationFiles() {
        return animationFiles;
    }

    public Map<String, NativeTexture> textures() {
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
