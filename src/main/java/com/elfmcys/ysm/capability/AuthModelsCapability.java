package com.elfmcys.ysm.capability;

import com.google.common.collect.Sets;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Set;

public class AuthModelsCapability {
    private Set<String> authModels = Sets.newHashSet();

    public void addModel(String modelId) {
        authModels.add(modelId);
    }

    public void copyFrom(AuthModelsCapability source) {
        this.authModels = source.authModels;
    }

    public void removeModel(String modelId) {
        authModels.remove(modelId);
    }

    public boolean containModel(String modelId) {
        return authModels.contains(modelId);
    }

    public Set<String> getAuthModels() {
        return authModels;
    }

    public void setAuthModels(Set<String> authModels) {
        this.authModels = authModels;
    }

    public void clear() {
        authModels.clear();
    }

    public ListTag serializeNBT() {
        ListTag listTag = new ListTag();
        for (String modelId : authModels) {
            listTag.add(StringTag.valueOf(modelId));
        }
        return listTag;
    }

    public void deserializeNBT(ListTag nbt) {
        this.authModels.clear();
        for (Tag tag : nbt) {
            authModels.add(tag.getAsString());
        }
    }
}
