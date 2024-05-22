package com.elfmcys.yesstevemodel.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Native Access
public class SyncModelResult {
    private final boolean success;
    // success 为 false 时有意义
    @Nullable
    private final Component error;
    private final Set<UUID> allPlayerIds;
    // success 为 true 时有意义
    @Nullable
    private final Map<UUID, Component> playerErrors;

    // Native Access
    @SuppressWarnings("unchecked")
    public SyncModelResult(boolean success, @Nullable Object error, UUID[] allPlayerIds, @Nullable Map<UUID, Object> playerErrors) {
        this.success = success;
        this.error = (Component) error;
        this.allPlayerIds = ImmutableSet.copyOf(allPlayerIds);
        this.playerErrors = playerErrors == null ? null : ImmutableMap.copyOf((Map<UUID, Component>)(Object)playerErrors);
    }

    public boolean success() {
        return success;
    }

    public Component error() {
        return error;
    }

    public Set<UUID> allPlayerIds() {
        return allPlayerIds;
    }

    public Map<UUID, Component> playerErrors() {
        return playerErrors;
    }
}
