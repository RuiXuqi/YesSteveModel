package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import net.minecraft.nbt.CompoundTag;

public class ArrowModelInfoCapability {
    private String modelId = ModelIdUtil.DEFAULT_MODEL_ID;
    private boolean initialized = false;

    public void init(String modelId) {
        this.modelId = modelId;
        this.initialized = true;
    }

    public void copyFrom(ArrowModelInfoCapability source) {
        this.modelId = source.modelId;
        this.initialized = source.initialized;
    }

    public String getOwnerModelId() {
        return modelId;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("owner_model_id", modelId);
        tag.putBoolean("initialized", initialized);
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        this.modelId = ModelIdUtil.stripLegacyPrefix(nbt.getString("owner_model_id"));
        this.initialized = nbt.getBoolean("initialized");
    }
}
