package com.elfmcys.yesstevemodel.client.model;

import com.elfmcys.yesstevemodel.client.texture.NativeTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ClientModelInfo {
    private final String name;
    private boolean needAuth;
    private final Map<String, NativeTexture> authorAvatars;
    private final Map<String, AbstractTexture> guiImages;

    public ClientModelInfo(String name, boolean needAuth,
                           Map<String, NativeTexture> authorAvatars, Map<String, AbstractTexture> guiImages) {
        this.name = name;
        this.needAuth = needAuth;
        this.authorAvatars = authorAvatars;
        this.guiImages = guiImages;
    }

    public String name() {
        return name;
    }

    public boolean isNeedAuth() {
        return needAuth;
    }

    public void setNeedAuth(boolean needAuth) {
        this.needAuth = needAuth;
    }

    public Map<String, NativeTexture> authorAvatars() {
        return authorAvatars;
    }

    @Nullable
    public AbstractTexture guiForeground() {
        return guiImages.get("gui_foreground");
    }

    @Nullable
    public AbstractTexture guiBackground() {
        return guiImages.get("gui_background");
    }
}
