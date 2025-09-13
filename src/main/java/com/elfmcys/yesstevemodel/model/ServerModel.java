package com.elfmcys.yesstevemodel.model;

import com.elfmcys.yesstevemodel.info.ModelInfo;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

// Native Access
@SuppressWarnings("removal")
public class ServerModel {
    private final String name;
    private final ServerPlayerModel playerModel;
    private final Set<ResourceLocation> projectileModels;
    private final Set<ResourceLocation> vehicleModels;
    private final ModelInfo info;
    private final boolean isDefault;
    private final boolean isNeedAuth;

    // Native Access
    public ServerModel(String name, ServerPlayerModel playerModel, String[] projectileModels, String[] vehicleModels,
                       ModelInfo info, boolean isDefault, boolean isNeedAuth) {
        this.name = name;
        this.playerModel = playerModel;
        this.projectileModels = Arrays.stream(projectileModels).map(ResourceLocation::new).collect(Collectors.toSet());
        this.vehicleModels = Arrays.stream(vehicleModels).map(ResourceLocation::new).collect(Collectors.toSet());
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
        return projectileModels;
    }

    public Set<ResourceLocation> vehicleModels() {
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
