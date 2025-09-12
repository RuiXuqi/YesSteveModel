package com.elfmcys.yesstevemodel.client.model;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ClientModelInfo {
    private final String name;
    private boolean needAuth;
    private final Map<String, ResourceLocation> authorAvatars;
    private final Map<String, ResourceLocation> guiImages;

    public ClientModelInfo(String name, boolean needAuth,
                           Map<String, ResourceLocation> authorAvatars, Map<String, ResourceLocation> guiImages) {
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

    public Map<String, ResourceLocation> authorAvatars() {
        return authorAvatars;
    }

    @Nullable
    public ResourceLocation guiForeground() {
        return guiImages.get("gui_foreground");
    }

    @Nullable
    public ResourceLocation guiBackground() {
        return guiImages.get("gui_background");
    }
}
