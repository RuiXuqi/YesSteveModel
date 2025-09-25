package com.elfmcys.yesstevemodel.mixin;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.TlmCommonCompat;
import com.elfmcys.yesstevemodel.event.CapabilityEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public class ProjectileEntityMixin {
    @Inject(at = @At("RETURN"), method = "setOwner(Lnet/minecraft/world/entity/Entity;)V")
    private void setOwner(Entity owner, CallbackInfo callbackInfo) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        // 仅在服务端执行
        Projectile projectile = (Projectile) (Object) this;
        if (projectile == null || projectile.level() == null || projectile.level().isClientSide()) {
            return;
        }
        if (owner instanceof ServerPlayer) {
            CapabilityEvent.onProjectileSetOwner(projectile, (ServerPlayer) owner);
        } else if (TlmCommonCompat.isMaid(owner)) {
            TlmCommonCompat.onProjectileSetOwner(projectile, owner);
        }
    }
}
