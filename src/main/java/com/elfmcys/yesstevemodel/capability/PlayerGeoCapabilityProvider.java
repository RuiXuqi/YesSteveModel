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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PlayerGeoCapabilityProvider implements ICapabilityProvider {
    public static Capability<PlayerGeoCapability> CAP = CapabilityManager.get(new CapabilityToken<>() {
    });
    private PlayerGeoCapability instance;
    private AbstractClientPlayer player;

    public PlayerGeoCapabilityProvider(AbstractClientPlayer player) {
        this.player = player;
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return getCapability(cap);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        return CAP.orEmpty(cap, LazyOptional.of(this::createCapability));
    }

    @NotNull
    private PlayerGeoCapability createCapability() {
        if (instance == null) {
            this.instance = new PlayerGeoCapability(player);
            player = null;
        }
        return instance;
    }
}
