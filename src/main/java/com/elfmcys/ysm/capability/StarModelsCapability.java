package com.elfmcys.ysm.capability;

import com.elfmcys.ysm.model.domain.ModelHash;
import com.google.common.collect.Sets;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Set;

public class StarModelsCapability {
    private Set<ModelHash> starModels = Sets.newHashSet();

    public void addModel(ModelHash modelHash) {
        starModels.add(modelHash);
    }

    public void copyFrom(StarModelsCapability source) {
        this.starModels = source.starModels;
    }

    public void removeModel(ModelHash modelHash) {
        starModels.remove(modelHash);
    }

    public boolean containModel(ModelHash modelHash) {
        return starModels.contains(modelHash);
    }

    public Set<ModelHash> getStarModels() {
        return starModels;
    }

    public void setStarModels(Set<ModelHash> starModels) {
        this.starModels = Sets.newHashSet(starModels);
    }

    public void clear() {
        starModels.clear();
    }

    public ListTag serializeNBT() {
        ListTag listTag = new ListTag();
        for (ModelHash modelHash : starModels) {
            listTag.add(StringTag.valueOf(modelHash.toString()));
        }
        return listTag;
    }

    public void deserializeNBT(ListTag nbt) {
        this.starModels.clear();
        for (Tag tag : nbt) {
            try {
                starModels.add(ModelHash.parse(tag.getAsString()));
            } catch (IllegalArgumentException ignored) {
                // Old path-based favorites are intentionally not migrated.
            }
        }
    }
}
