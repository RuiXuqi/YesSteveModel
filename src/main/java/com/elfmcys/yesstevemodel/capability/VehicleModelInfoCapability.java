package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.nbt.CompoundTag;

public class VehicleModelInfoCapability {
    private String modelId = ModelIdUtil.DEFAULT_MODEL_ID;
    private boolean initialized = false;
    private Object2FloatOpenHashMap<String> molangVarsServerBound = new Object2FloatOpenHashMap<>();

    public void init(String modelId, Object2FloatOpenHashMap<String> molangVarsServerBound) {
        this.modelId = modelId;
        this.initialized = true;
        this.molangVarsServerBound = molangVarsServerBound;
    }

    public void copyFrom(VehicleModelInfoCapability source) {
        this.modelId = source.modelId;
        this.initialized = source.initialized;
        this.molangVarsServerBound = source.molangVarsServerBound;
    }

    public String getOwnerModelId() {
        return modelId;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Object2FloatOpenHashMap<String> getMolangVarsServerBound() {
        return molangVarsServerBound;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("owner_model_id", modelId);
        tag.putBoolean("initialized", initialized);

        CompoundTag varsTag = new CompoundTag();
        molangVarsServerBound.object2FloatEntrySet().fastForEach(varsEntry -> {
            varsTag.putFloat(varsEntry.getKey(), varsEntry.getFloatValue());
        });
        tag.put("molang_vars_server_bound", varsTag);

        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        this.modelId = nbt.getString("owner_model_id");
        this.initialized = nbt.getBoolean("initialized");

        this.molangVarsServerBound.clear();
        var varsTag = nbt.getCompound("molang_vars_server_bound");
        for (var name : varsTag.getAllKeys()) {
            var value = varsTag.getFloat(name);
            this.molangVarsServerBound.put(name, value);
        }
    }
}
