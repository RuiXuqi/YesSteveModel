package com.elfmcys.yesstevemodel.client.model.data;

import com.elfmcys.yesstevemodel.client.texture.NativeTexture;
import com.elfmcys.yesstevemodel.info.ModelInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

// Native Access
public final class ClientModelData {
    private final PlayerModelData playerModel;
    private final Map<String, ProjectileModelData> projectileModel;
    private final CommonAssetData assets;

    @NotNull
    private final ModelInfo info;
    private final Map<String, NativeTexture> authorAvatars;

    // Native Access
    public ClientModelData(PlayerModelData playerModel, Map<String, ProjectileModelData> projectileModel, CommonAssetData assets, @NotNull ModelInfo info, Map<String, NativeTexture> authorAvatars) {
        this.playerModel = playerModel;
        this.projectileModel = projectileModel;
        this.assets = assets;
        this.info = info;
        this.authorAvatars = authorAvatars;
    }

    public PlayerModelData playerModel() {
        return playerModel;
    }

    public Map<String, ProjectileModelData> projectileModel() {
        return projectileModel;
    }

    public CommonAssetData assets() {
        return assets;
    }

    public Map<String, NativeTexture> authorAvatars() {
        return authorAvatars;
    }

    @NotNull
    public ModelInfo info() {
        return info;
    }
}
