package com.elfmcys.yesstevemodel.client.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public class ClientModelInfo {
    private final String name;
    private final List<Component> displayInfo;
    private boolean needAuth;
    private final Map<String, ResourceLocation> authorAvatars;

    public ClientModelInfo(String name, List<Component> displayInfo, boolean needAuth, Map<String, ResourceLocation> authorAvatars) {
        this.name = name;
        this.displayInfo = displayInfo;
        this.needAuth = needAuth;
        this.authorAvatars = authorAvatars;
    }

    public String name() {
        return name;
    }

    public List<Component> displayInfo() {
        return displayInfo;
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
}
