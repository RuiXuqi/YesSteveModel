package com.elfmcys.yesstevemodel.capability;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ArrowGeoCapabilityProvider implements ICapabilityProvider {
    public static Capability<ArrowGeoCapability> CAP = CapabilityManager.get(new CapabilityToken<>() {
    });
    private ArrowGeoCapability instance;
    private AbstractArrow arrow;

    public ArrowGeoCapabilityProvider(AbstractArrow arrow) {
        this.arrow = arrow;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return CAP.orEmpty(cap, LazyOptional.of(this::createCapability));
    }

    @NotNull
    private ArrowGeoCapability createCapability() {
        if (instance == null) {
            this.instance = new ArrowGeoCapability(arrow);
            arrow = null;
        }
        return instance;
    }
}
