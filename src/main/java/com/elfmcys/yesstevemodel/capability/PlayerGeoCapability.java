package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.compat.IrisCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.instance.CustomPlayerInstance;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.CameraType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerGeoCapability extends CustomPlayerInstance {
    private AnimationEvent<CustomPlayerEntity> lastEvent;

    public PlayerGeoCapability(AbstractClientPlayer player) {
        super(player, true, player instanceof LocalPlayer);
    }

    private boolean isFirstPersonModActive() {
        if (animatable.getEntity() instanceof LocalPlayer) {
            // 在第一人称下，如果安装了第一人称模组并启用，则异步更新是多余的
            return Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON && FirstPersonCompat.isInstalled() && FirstPersonCompat.isEnabled();
        }
        return false;
    }

    @Override
    public boolean canUpdateAsync() {
        return !isFirstPersonModActive();
    }

    private boolean shouldSkipShadowRenderPass() {
        return isFirstPersonModActive() && IrisCompat.isInstalled() && IrisCompat.isRenderingShadow() && lastEvent != null;
    }

    @Override
    public AnimationEvent<CustomPlayerEntity> syncUpdate(float partialTicks) {
        if (shouldSkipShadowRenderPass()) {
            animatableModel.codeAnimationForShadowRendering();
            return lastEvent;
        }
        return lastEvent = super.syncUpdate(partialTicks);
    }

    @Override
    public AnimationEvent<CustomPlayerEntity> waitOrUpdate(float partialTicks) {
        if (shouldSkipShadowRenderPass()) {
            animatableModel.codeAnimationForShadowRendering();
            return lastEvent;
        }
        return lastEvent = super.waitOrUpdate(partialTicks);
    }
}
