package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class ArrowModelInfoCapability {
    private ResourceLocation ownerModelId = ModelIdUtil.DEFAULT_MODEL_ID;
    private boolean initialized = false;

    public void init(ResourceLocation ownerModelId) {
        this.ownerModelId = ownerModelId;
        this.initialized = true;
    }

    public void copyFrom(ArrowModelInfoCapability source) {
        this.ownerModelId = source.ownerModelId;
        this.initialized = source.initialized;
    }

    public ResourceLocation getOwnerModelId() {
        return ownerModelId;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("owner_model_id", this.ownerModelId.toString());
        tag.putBoolean("initialized", initialized);
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        var ownerModelId = ResourceLocation.tryParse(nbt.getString("owner_model_id"));
        if (ownerModelId != null) {
            this.ownerModelId = ownerModelId;
            this.initialized = nbt.getBoolean("initialized");
        }
    }
}
