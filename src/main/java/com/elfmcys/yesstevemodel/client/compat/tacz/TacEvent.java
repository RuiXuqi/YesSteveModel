package com.elfmcys.yesstevemodel.client.compat.tacz;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.TlmCompat;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.api.event.common.GunReloadEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TacEvent {
    @SubscribeEvent
    public void onGunShoot(GunFireEvent event) {
        LivingEntity shooter = event.getShooter();
        shooter.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
            cap.setTacGunAnimationNeedReload(true);
        });
        TlmCompat.markTacGunAnimationNeedReload(shooter);
    }

    @SubscribeEvent
    public void onGunShoot(GunMeleeEvent event) {
        LivingEntity shooter = event.getShooter();
        shooter.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
            cap.setTacGunAnimationNeedReload(true);
        });
        TlmCompat.markTacGunAnimationNeedReload(shooter);
    }

    @SubscribeEvent
    public void onGunShoot(GunReloadEvent event) {
        LivingEntity shooter = event.getEntity();
        shooter.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
            cap.setTacGunAnimationNeedReload(true);
        });
        TlmCompat.markTacGunAnimationNeedReload(shooter);
    }
}
