package com.elfmcys.ysm.client.model.data;

import com.elfmcys.ysm.client.texture.NativeTexture;
import com.elfmcys.ysm.info.ModelInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

// Native Access
public final class ClientModelData {
    private final PlayerModelData playerModel;
    private final ProjectileModelData[] projectileModels;
    private final VehicleModelData[] vehicleModels;
    private final CommonAssetData assets;

    @NotNull
    private final ModelInfo info;
    private final Map<String, NativeTexture> authorAvatars;
    private final Map<String, NativeTexture> guiImages;

    // Native Access
    public ClientModelData(PlayerModelData playerModel, ProjectileModelData[] projectileModels, VehicleModelData[] vehicleModels,
                           CommonAssetData assets, @NotNull ModelInfo info, Map<String, NativeTexture> authorAvatars, Map<String, NativeTexture> guiImages) {
        this.playerModel = playerModel;
        this.projectileModels = projectileModels;
        this.vehicleModels = vehicleModels;
        this.assets = assets;
        this.info = info;
        this.authorAvatars = authorAvatars;
        this.guiImages = guiImages;
    }

    public PlayerModelData playerModel() {
        return playerModel;
    }

    public ProjectileModelData[] projectileModel() {
        return projectileModels;
    }

    public VehicleModelData[] vehicleModel() {
        return vehicleModels;
    }

    public CommonAssetData assets() {
        return assets;
    }

    public Map<String, NativeTexture> authorAvatars() {
        return authorAvatars;
    }

    /**
     * 目前就两个可能存在的图片
     * gui_foreground 和 gui_background
     */
    public Map<String, NativeTexture> guiImages() {
        return guiImages;
    }

    @NotNull
    public ModelInfo info() {
        return info;
    }
}
