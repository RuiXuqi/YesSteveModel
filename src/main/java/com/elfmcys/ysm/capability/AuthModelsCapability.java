package com.elfmcys.ysm.capability;

import com.elfmcys.ysm.model.domain.ModelHash;
import com.google.common.collect.Sets;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Set;

public class AuthModelsCapability {
    private Set<ModelHash> authModels = Sets.newHashSet();

    public void addModel(ModelHash modelHash) {
        authModels.add(modelHash);
    }

    public void copyFrom(AuthModelsCapability source) {
        this.authModels = source.authModels;
    }

    public void removeModel(ModelHash modelHash) {
        authModels.remove(modelHash);
    }

    public boolean containModel(ModelHash modelHash) {
        return authModels.contains(modelHash);
    }

    public Set<ModelHash> getAuthModels() {
        return authModels;
    }

    public void setAuthModels(Set<ModelHash> authModels) {
        this.authModels = Sets.newHashSet(authModels);
    }

    public void clear() {
        authModels.clear();
    }

    public ListTag serializeNBT() {
        ListTag listTag = new ListTag();
        for (ModelHash modelHash : authModels) {
            listTag.add(StringTag.valueOf(modelHash.toString()));
        }
        return listTag;
    }

    public void deserializeNBT(ListTag nbt) {
        this.authModels.clear();
        for (Tag tag : nbt) {
            try {
                authModels.add(ModelHash.parse(tag.getAsString()));
            } catch (IllegalArgumentException ignored) {
                // Old path-based authorization entries are intentionally not migrated.
            }
        }
    }
}
