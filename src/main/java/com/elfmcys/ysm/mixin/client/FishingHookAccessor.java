package com.elfmcys.ysm.mixin.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FishingHook.class)
public interface FishingHookAccessor {
    @Accessor("biting")
    boolean ysm$IsBiting();

    @Accessor("hookedIn")
    Entity ysm$GetHookedIn();
}
