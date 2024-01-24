package com.elfmcys.yesstevemodel.geckolib3.resource;

import com.elfmcys.yesstevemodel.geckolib3.file.AnimationFile;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.google.common.collect.Maps;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class GeckoLibCache {
    private final static GeckoLibCache INSTANCE = new GeckoLibCache();
    private Map<ResourceLocation, AnimationFile> animations = Maps.newHashMap();
    private Map<ResourceLocation, GeoModel> geoModels = Maps.newHashMap();

    public static GeckoLibCache getInstance() {
        return INSTANCE;
    }

    public void setAll(Map<ResourceLocation, GeoModel> geoModels, Map<ResourceLocation, AnimationFile> animations) {
        this.geoModels = geoModels;
        this.animations = animations;
    }

    public Map<ResourceLocation, AnimationFile> getAnimations() {
        return animations;
    }

    public Map<ResourceLocation, GeoModel> getGeoModels() {
        return geoModels;
    }
}
