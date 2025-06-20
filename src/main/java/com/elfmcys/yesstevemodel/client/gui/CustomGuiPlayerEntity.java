package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapability;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class CustomGuiPlayerEntity extends CustomPlayerEntity {
    public CustomGuiPlayerEntity() {
        super(Minecraft.getInstance().player, false, false);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    protected AnimationEvent<?> performUpdate(float partialTicks) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        setPlayer(player);
        return super.performUpdate(partialTicks);
    }

    public void setPlayer(LocalPlayer player) {
        entity = player;
    }

    public void waitForCapabilityUpdate() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(PlayerAnimatableCapability::waitForAsyncUpdate);
    }
}
