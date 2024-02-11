package com.elfmcys.yesstevemodel.geckolib3.model.provider;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.resource.GeckoLibCache;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public abstract class GeoModelProvider<T> {
    public double seekTime;
    public double lastGameTickTime;
    public boolean shouldCrashOnMissing = false;

    public GeoModel getModel(ResourceLocation location) {
        return GeckoLibCache.getInstance().getGeoModels().get(location);
    }

    public abstract ResourceLocation getModelLocation(T object);

    public abstract ResourceLocation getTextureLocation(T object);

    @Nullable
    public Struct getRemoteStruct(T object) {
        return null;
    }
}
