package com.elfmcys.yesstevemodel.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ModelInfoCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
    public static Capability<ModelInfoCapability> MODEL_INFO_CAP = CapabilityManager.get(new CapabilityToken<>() {
    });
    private ModelInfoCapability instance = null;

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == MODEL_INFO_CAP) {
            return LazyOptional.of(this::createCapability).cast();
        }
        return LazyOptional.empty();
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        return getCapability(cap, null);
    }

    @Nonnull
    private ModelInfoCapability createCapability() {
        if (instance == null) {
            this.instance = new ModelInfoCapability();
        }
        return instance;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createCapability().deserializeNBT(nbt);
    }

    @Override
    public CompoundTag serializeNBT() {
        return createCapability().serializeNBT();
    }
}
