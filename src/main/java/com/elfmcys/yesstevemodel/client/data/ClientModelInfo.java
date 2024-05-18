package com.elfmcys.yesstevemodel.client.data;

import net.minecraft.network.chat.Component;

import java.util.List;

public class ClientModelInfo {
    private final List<Component> displayInfo;
    private final boolean needAuth;

    public ClientModelInfo(List<Component> displayInfo, boolean needAuth) {
        this.displayInfo = displayInfo;
        this.needAuth = needAuth;
    }

    public List<Component> displayInfo() {
        return displayInfo;
    }

    public boolean isNeedAuth() {
        return needAuth;
    }
}
