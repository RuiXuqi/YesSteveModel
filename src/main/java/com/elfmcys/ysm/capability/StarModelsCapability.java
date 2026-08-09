package com.elfmcys.ysm.capability;

import com.elfmcys.ysm.model.domain.Hash256;
import com.google.common.collect.Sets;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Set;

public class StarModelsCapability {
    private Set<Hash256> starModels = Sets.newHashSet();

    public void addModel(Hash256 modelHash) {
        starModels.add(modelHash);
    }

    public void copyFrom(StarModelsCapability source) {
        this.starModels = source.starModels;
    }

    public void removeModel(Hash256 modelHash) {
        starModels.remove(modelHash);
    }

    public boolean containModel(Hash256 modelHash) {
        return starModels.contains(modelHash);
    }

    public Set<Hash256> getStarModels() {
        return starModels;
    }

    public void setStarModels(Set<Hash256> starModels) {
        this.starModels = Sets.newHashSet(starModels);
    }

    public void clear() {
        starModels.clear();
    }

    public ListTag serializeNBT() {
        ListTag listTag = new ListTag();
        for (Hash256 modelHash : starModels) {
            listTag.add(StringTag.valueOf(modelHash.toString()));
        }
        return listTag;
    }

    public void deserializeNBT(ListTag nbt) {
        this.starModels.clear();
        for (Tag tag : nbt) {
            try {
                starModels.add(Hash256.parse(tag.getAsString()));
            } catch (IllegalArgumentException ignored) {
                // Old path-based favorites are intentionally not migrated.
            }
        }
    }
}
