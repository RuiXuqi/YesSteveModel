package com.elfmcys.yesstevemodel.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.Set;

// Native Access
public class ReloadModelResult {
    private final boolean success;
    // success 为 true 时可能有意义，false 时必有意义
    @Nullable
    private final Component message;
    private final Map<String, ServerModel> models;
    private final Set<String> authModels;

    // Native Access
    public ReloadModelResult(boolean success, @Nullable Object message, Map<String, ServerModel> models, String[] authModels) {
        this.success = success;
        this.message = (Component) message;
        this.models = models == null ? Object2ReferenceMaps.emptyMap() : ImmutableMap.copyOf(models);
        this.authModels = authModels == null ? ObjectSets.emptySet() : ImmutableSet.copyOf(authModels);
    }

    public boolean success() {
        return success;
    }

    @Nullable
    public Component message() {
        return message;
    }

    public Map<String, ServerModel> models() {
        return models;
    }

    public Set<String> authModels() {
        return authModels;
    }
}
