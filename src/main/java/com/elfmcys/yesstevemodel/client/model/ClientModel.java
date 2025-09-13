package com.elfmcys.yesstevemodel.client.model;

import com.elfmcys.yesstevemodel.info.ModelInfo;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public class ClientModel {
    private final PlayerModel playerModel;
    private final Map<ResourceLocation, ProjectileModel> projectileModels;
    private final Map<ResourceLocation, VehicleModel> vehicleModels;
    private final CommonAsset assets;

    private final ModelInfo modelInfo;
    private final ClientModelInfo clientModelInfo;
    private final List<ResourceLocation> registeredTextureIds;

    public ClientModel(PlayerModel playerModel, Map<ResourceLocation, ProjectileModel> projectileModels, Map<ResourceLocation, VehicleModel> vehicleModels,
                       CommonAsset assets, ModelInfo modelInfo, ClientModelInfo clientModelInfo, List<ResourceLocation> registeredTextureIds) {
        this.playerModel = playerModel;
        this.projectileModels = projectileModels;
        this.vehicleModels = vehicleModels;
        this.assets = assets;
        this.modelInfo = modelInfo;
        this.clientModelInfo = clientModelInfo;
        this.registeredTextureIds = registeredTextureIds;
    }

    public PlayerModel playerModel() {
        return playerModel;
    }

    public List<ResourceLocation> registeredTextureIds() {
        return registeredTextureIds;
    }

    public CommonAsset assets() {
        return assets;
    }

    public Map<ResourceLocation, ProjectileModel> projectileModels() {
        return projectileModels;
    }

    public Map<ResourceLocation, VehicleModel> vehicleModels() {
        return vehicleModels;
    }

    public ModelInfo info() {
        return modelInfo;
    }

    public ClientModelInfo clientInfo() {
        return clientModelInfo;
    }
}
