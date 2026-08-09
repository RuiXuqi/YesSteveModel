package com.elfmcys.ysm.capability;

import com.elfmcys.ysm.model.domain.Hash256;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.nbt.CompoundTag;

public class VehicleModelInfoCapability {
    private Hash256 modelHash;
    private boolean initialized = false;
    private Object2FloatOpenHashMap<String> molangVarsServerBound = new Object2FloatOpenHashMap<>();

    public void update(Hash256 modelHash, Object2FloatOpenHashMap<String> molangVarsServerBound) {
        this.modelHash = modelHash;
        this.initialized = true;
        this.molangVarsServerBound = molangVarsServerBound;
    }

    public void copyFrom(VehicleModelInfoCapability source) {
        this.modelHash = source.modelHash;
        this.initialized = source.initialized;
        this.molangVarsServerBound = source.molangVarsServerBound;
    }

    public Hash256 getOwnerModelHash() {
        return modelHash;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Object2FloatOpenHashMap<String> getMolangVarsServerBound() {
        return molangVarsServerBound;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("owner_model_hash", modelHash == null ? "" : modelHash.toString());
        tag.putBoolean("initialized", initialized);

        CompoundTag varsTag = new CompoundTag();
        molangVarsServerBound.object2FloatEntrySet().fastForEach(varsEntry -> {
            varsTag.putFloat(varsEntry.getKey(), varsEntry.getFloatValue());
        });
        tag.put("molang_vars_server_bound", varsTag);

        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        var stored = nbt.getString("owner_model_hash");
        try {
            this.modelHash = stored.isEmpty() ? null : Hash256.parse(stored);
        } catch (IllegalArgumentException ignored) {
            this.modelHash = null;
        }
        this.initialized = nbt.getBoolean("initialized");

        this.molangVarsServerBound.clear();
        var varsTag = nbt.getCompound("molang_vars_server_bound");
        for (var name : varsTag.getAllKeys()) {
            var value = varsTag.getFloat(name);
            this.molangVarsServerBound.put(name, value);
        }
    }
}
