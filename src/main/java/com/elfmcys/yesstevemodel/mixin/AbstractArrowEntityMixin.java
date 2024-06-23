package com.elfmcys.yesstevemodel.mixin;

import com.elfmcys.yesstevemodel.api.IArrowExtraInfo;
import com.elfmcys.yesstevemodel.event.CapabilityEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public class AbstractArrowEntityMixin implements IArrowExtraInfo {
    @Shadow
    protected boolean inGround;
    @Shadow
    protected int inGroundTime;

    @Unique
    @Override
    public boolean isInGround() {
        return inGround;
    }

    @Unique
    @Override
    public int inGroundTime() {
        return inGroundTime;
    }

    // 仅在服务端执行
    @Inject(at = @At("RETURN"), method = "setOwner(Lnet/minecraft/world/entity/Entity;)V")
    private void setOwner(Entity owner, CallbackInfo callbackInfo) {
        if (owner instanceof ServerPlayer) {
            CapabilityEvent.onArrowSetOwner((AbstractArrow) (Object) this, (ServerPlayer) owner);
        }
    }
}
