package com.elfmcys.yesstevemodel.capability;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class PlayerGeoCapabilityProvider implements ICapabilityProvider {
    public static Capability<PlayerGeoCapability> CAP = CapabilityManager.get(new CapabilityToken<>() {
    });
    private LazyOptional<PlayerGeoCapability> holder;
    private AbstractClientPlayer player;

    public PlayerGeoCapabilityProvider(AbstractClientPlayer player) {
        this.player = player;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return getCapability(cap);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap) {
        if(cap != CAP) {
            return LazyOptional.empty();
        }
        if(holder == null) {
            final PlayerGeoCapability instance = new PlayerGeoCapability(player);
            this.holder = LazyOptional.of(() -> instance);
            player = null;
        }
        return this.holder.cast();
    }
}
