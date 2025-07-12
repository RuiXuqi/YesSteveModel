package com.elfmcys.yesstevemodel.mixin;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.api.IArrowExtraInfo;
import com.elfmcys.yesstevemodel.event.CapabilityEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public class AbstractArrowEntityMixin implements IArrowExtraInfo {
    @Unique
    private String shootItemId = StringUtils.EMPTY;
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

    @Unique
    @Override
    public String getShootItemId() {
        return shootItemId;
    }

    @Inject(at = @At("RETURN"), method = "setOwner(Lnet/minecraft/world/entity/Entity;)V")
    private void setOwner(Entity owner, CallbackInfo callbackInfo) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        // 仅在服务端执行
        if (owner instanceof ServerPlayer) {
            CapabilityEvent.onArrowSetOwner((AbstractArrow) (Object) this, (ServerPlayer) owner);
        }
        // 设置 owner 时，缓存一下射击时主手物品 ID，用于 molang
        if (owner instanceof LivingEntity livingEntity) {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(livingEntity.getMainHandItem().getItem());
            if (key != null) {
                shootItemId = key.toString();
            }
        }
    }
}
