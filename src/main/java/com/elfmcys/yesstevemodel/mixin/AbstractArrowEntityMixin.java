package com.elfmcys.yesstevemodel.mixin;

import com.elfmcys.yesstevemodel.api.IArrowExtraInfo;
import com.elfmcys.yesstevemodel.util.Keep;
import com.elfmcys.yesstevemodel.util.MixinWrapper;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public class AbstractArrowEntityMixin implements IArrowExtraInfo {
    @Unique
    private static final EntityDataAccessor<String> MODEL_NAME_PARAM = SynchedEntityData.defineId(AbstractArrow.class, EntityDataSerializers.STRING);
    @Unique
    private static final String MODEL_NAME_TAG = "YsmArrowModelName";
    @Unique
    private boolean empty = false;
    @Unique
    private Object instance = null;

    @Shadow
    @Keep
    protected boolean inGround;
    @Shadow
    @Keep
    protected int inGroundTime;

    // 仅在客户端执行
    @Unique
    @Override
    public Object getGeoInstance() {
        if(empty) {
            return null;
        }
        if (instance != null) {
            return instance;
        }

        String modelName = getYsmModelName();
        if(modelName.equals(IArrowExtraInfo.EMPTY_MODEL_NAME)) {
            empty = true;
            return null;
        }

        instance = MixinWrapper.getInstance((AbstractArrow) (Object) this, modelName);
        if(instance == null) {
            empty = true;
            return null;
        }

        return instance;
    }

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

    @Inject(at = @At("RETURN"), method = "defineSynchedData()V")
    private void defineSynchedData(CallbackInfo callbackInfo) {
        getYsmEntityData().define(MODEL_NAME_PARAM, IArrowExtraInfo.EMPTY_MODEL_NAME);
    }

    // 仅在服务端执行
    @Inject(at = @At("RETURN"), method = "setOwner(Lnet/minecraft/world/entity/Entity;)V")
    private void setOwner(Entity entity, CallbackInfo callbackInfo) {
        if (entity instanceof ServerPlayer && getYsmModelName().equals(IArrowExtraInfo.EMPTY_MODEL_NAME)) {
            MixinWrapper.getPlayerModelName(entity, this::setYsmModelName);
        }
    }

    @Inject(at = @At("RETURN"), method = "addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V")
    private void addAdditionalSaveData(CompoundTag pCompound, CallbackInfo callbackInfo) {
        pCompound.putString(MODEL_NAME_TAG, getYsmModelName());
    }

    @Inject(at = @At("RETURN"), method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V")
    private void readAdditionalSaveData(CompoundTag pCompound, CallbackInfo callbackInfo) {
        Tag modelNameTag = pCompound.get(MODEL_NAME_TAG);
        if (modelNameTag instanceof StringTag) {
            setYsmModelName(modelNameTag.getAsString());
        }
    }

    @Unique
    private String getYsmModelName() {
        return getYsmEntityData().get(MODEL_NAME_PARAM);
    }

    @Unique
    private void setYsmModelName(String modelName) {
        getYsmEntityData().set(MODEL_NAME_PARAM, modelName);
    }

    @Unique
    private SynchedEntityData getYsmEntityData() {
        return ((Entity) (Object) this).getEntityData();
    }
}
