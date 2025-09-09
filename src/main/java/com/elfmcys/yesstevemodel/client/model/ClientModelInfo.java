package com.elfmcys.yesstevemodel.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public class ClientModelInfo {
    private final String name;
    private final Map<String, List<Component>> displayInfo;
    private boolean needAuth;
    private final Map<String, ResourceLocation> authorAvatars;

    public ClientModelInfo(String name, Map<String, List<Component>> displayInfo, boolean needAuth, Map<String, ResourceLocation> authorAvatars) {
        this.name = name;
        this.displayInfo = displayInfo;
        this.needAuth = needAuth;
        this.authorAvatars = authorAvatars;
    }

    public String name() {
        return name;
    }

    public List<Component> displayInfo() {
        String selected = Minecraft.getInstance().getLanguageManager().getSelected();
        return displayInfo.getOrDefault(selected, displayInfo.get("en_us"));
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
