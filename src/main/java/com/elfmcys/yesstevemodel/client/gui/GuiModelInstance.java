package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapability;
import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.instance.CustomPlayerInstance;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class GuiModelInstance extends CustomPlayerInstance {
    public GuiModelInstance() {
        super(Minecraft.getInstance().player, false, false);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    protected AnimationEvent<CustomPlayerEntity> performUpdate(float partialTicks) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        animatable.setPlayer(player);
        return super.performUpdate(partialTicks);
    }

    public void waitForCapabilityUpdate() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(PlayerGeoCapability::waitForAsyncUpdate);
    }
}
