package com.elfmcys.ysm.capability;

import com.elfmcys.ysm.model.domain.Hash256;
import com.google.common.collect.Sets;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Set;

public class AuthModelsCapability {
    private Set<Hash256> authModels = Sets.newHashSet();

    public void addModel(Hash256 modelHash) {
        authModels.add(modelHash);
    }

    public void copyFrom(AuthModelsCapability source) {
        this.authModels = source.authModels;
    }

    public void removeModel(Hash256 modelHash) {
        authModels.remove(modelHash);
    }

    public boolean containModel(Hash256 modelHash) {
        return authModels.contains(modelHash);
    }

    public Set<Hash256> getAuthModels() {
        return authModels;
    }

    public void setAuthModels(Set<Hash256> authModels) {
        this.authModels = Sets.newHashSet(authModels);
    }

    public void clear() {
        authModels.clear();
    }

    public ListTag serializeNBT() {
        ListTag listTag = new ListTag();
        for (Hash256 modelHash : authModels) {
            listTag.add(StringTag.valueOf(modelHash.toString()));
        }
        return listTag;
    }

    public void deserializeNBT(ListTag nbt) {
        this.authModels.clear();
        for (Tag tag : nbt) {
            try {
                authModels.add(Hash256.parse(tag.getAsString()));
            } catch (IllegalArgumentException ignored) {
                // Old path-based authorization entries are intentionally not migrated.
            }
        }
    }
}
