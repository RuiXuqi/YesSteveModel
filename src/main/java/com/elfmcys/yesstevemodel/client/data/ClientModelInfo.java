package com.elfmcys.yesstevemodel.client.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Set;

public class ClientModelInfo {
    private final List<ResourceLocation> textureIds;
    // 模型哈希
    private final String hash;
    private final Set<String> features;
    private final double widthScale;
    private final double heightScale;
    private final List<Component> extraInfo;
    private final List<String> extraAnimationNames;
    private final boolean free;

    public ClientModelInfo(List<ResourceLocation> textureIds, String hash, Set<String> features, double widthScale, double heightScale, List<Component> extraInfo, List<String> extraAnimationNames, boolean free) {
        this.textureIds = ImmutableList.copyOf(textureIds);
        this.hash = hash;
        this.features = ImmutableSet.copyOf(features);
        this.widthScale = widthScale;
        this.heightScale = heightScale;
        this.extraInfo = ImmutableList.copyOf(extraInfo);
        this.extraAnimationNames = ImmutableList.copyOf(extraAnimationNames);
        this.free = free;
    }

    public List<ResourceLocation> textureIds() {
        return textureIds;
    }

    public String hash() {
        return hash;
    }

    public Set<String> features() {
        return features;
    }

    public double widthScale() {
        return widthScale;
    }

    public double heightScale() {
        return heightScale;
    }

    public List<Component> extraInfo() {
        return extraInfo;
    }

    public List<String> extraAnimationNames() {
        return extraAnimationNames;
    }

    public boolean isFree() {
        return free;
    }
}
