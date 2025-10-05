package com.elfmcys.yesstevemodel.model;

import com.elfmcys.yesstevemodel.info.ModelInfo;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

// Native Access
public class ServerModel {
    private final String name;
    private final ServerPlayerModel playerModel;
    private final Set<ResourceLocation> projectileModels;
    private final Set<ResourceLocation> vehicleModels;
    private final ModelInfo info;
    private final boolean isDefault;
    private final boolean isNeedAuth;

    private Object[] rawProjectileModels;
    private Object[] rawVehicleModels;

    // Native Access
    public ServerModel(String name, ServerPlayerModel playerModel, Object[] projectileModels, Object[] vehicleModels,
                       ModelInfo info, boolean isDefault, boolean isNeedAuth) {
        this.name = name;
        this.playerModel = playerModel;
        this.projectileModels = new HashSet<>();
        this.vehicleModels = new HashSet<>();
        this.rawProjectileModels = projectileModels;
        this.rawVehicleModels = vehicleModels;
        this.info = info;
        this.isDefault = isDefault;
        this.isNeedAuth = isNeedAuth;
    }

    public String name() {
        return name;
    }

    public ServerPlayerModel playerModel() {
        return playerModel;
    }

    public Set<ResourceLocation> projectileModels() {
        var raw = rawProjectileModels;
        for (var matches : raw) {
            projectileModels.addAll(ModelIdUtil.getEntityIdMatch((String[]) matches));
            rawProjectileModels = null;
        }
        return projectileModels;
    }

    public Set<ResourceLocation> vehicleModels() {
        var raw = rawVehicleModels;
        for (var matches : raw) {
            vehicleModels.addAll(ModelIdUtil.getEntityIdMatch((String[]) matches));
            rawVehicleModels = null;
        }
        return vehicleModels;
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
