package com.elfmcys.yesstevemodel.geckolib3.model.provider;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class GeoModelProvider<T> {
    public double seekTime;
    public double lastGameTickTime;
    public boolean shouldCrashOnMissing = false;

    public abstract GeoModel getModel(String location);

    public abstract String getModelLocation(T object);

    @NotNull
    public abstract ResourceLocation getTextureLocation(T object);

    @Nullable
    public Struct getRemoteStruct(T object) {
        return null;
    }
}
