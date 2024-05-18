package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.google.common.collect.Sets;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Set;
import java.util.stream.Collectors;

public class StarModelsCapability {
    private Set<String> starModels = Sets.newHashSet();

    public void addModel(String modelId) {
        starModels.add(modelId);
    }

    public void copyFrom(StarModelsCapability source) {
        this.starModels = source.starModels;
    }

    public void removeModel(String modelId) {
        starModels.remove(modelId);
    }

    public boolean containModel(String modelId) {
        return starModels.contains(modelId);
    }

    public Set<String> getStarModels() {
        return starModels;
    }

    public void setStarModels(Set<String> starModels) {
        this.starModels = starModels;
    }

    public void clear() {
        starModels.clear();
    }

    public ListTag serializeNBT() {
        ListTag listTag = new ListTag();
        for (String modelId : starModels) {
            listTag.add(StringTag.valueOf(modelId));
        }
        return listTag;
    }

    public void deserializeNBT(ListTag nbt) {
        this.starModels.clear();
        for (Tag tag : nbt) {
            starModels.add(tag.getAsString());
        }
        starModels = starModels.stream().map(ModelIdUtil::stripLegacyPrefix).collect(Collectors.toSet());
    }
}
