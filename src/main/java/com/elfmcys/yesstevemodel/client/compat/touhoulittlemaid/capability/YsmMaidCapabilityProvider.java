package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability;

import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.CustomYsmMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class YsmMaidCapabilityProvider implements ICapabilityProvider {
    public static final Capability<CustomYsmMaidEntity> CAP = CapabilityManager.get(new CapabilityToken<>() {
    });

    private CustomYsmMaidEntity instance;
    private final EntityMaid maid;

    public YsmMaidCapabilityProvider(EntityMaid maid) {
        this.maid = maid;
    }

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction direction) {
        return this.getCapability(capability);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        return CAP.orEmpty(cap, LazyOptional.of(this::createCapability));
    }

    private CustomYsmMaidEntity createCapability() {
        if (instance == null) {
            instance = new CustomYsmMaidEntity(this.maid, true);
        }
        return instance;
    }
}
