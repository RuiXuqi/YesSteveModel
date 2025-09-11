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
    private final Map<String, NativeTexture> guiImages;

    // Native Access
    public ClientModelData(PlayerModelData playerModel, Map<String, ProjectileModelData> projectileModel, CommonAssetData assets, @NotNull ModelInfo info,
                           Map<String, NativeTexture> authorAvatars, Map<String, NativeTexture> guiImages) {
        this.playerModel = playerModel;
        this.projectileModel = projectileModel;
        this.assets = assets;
        this.info = info;
        this.authorAvatars = authorAvatars;
        this.guiImages = guiImages;
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
