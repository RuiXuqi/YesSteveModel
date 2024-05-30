package com.elfmcys.yesstevemodel.client.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public class ClientModelInfo {
    private final List<Component> displayInfo;
    private final boolean needAuth;
    private final Map<String, ResourceLocation> authorAvatars;

    public ClientModelInfo(List<Component> displayInfo, boolean needAuth, Map<String, ResourceLocation> authorAvatars) {
        this.displayInfo = displayInfo;
        this.needAuth = needAuth;
        this.authorAvatars = authorAvatars;
    }

    public List<Component> displayInfo() {
        return displayInfo;
    }

    public boolean isNeedAuth() {
        return needAuth;
    }

    public Map<String, ResourceLocation> authorAvatars() {
        return authorAvatars;
    }
}
