package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.AnimationParallelTicker;
import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.util.ThreadTools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.concurrent.Future;

public abstract class GeoInstance<TAnimatable extends IAnimatable<?>, TModel extends AnimatedGeoModel<TAnimatable>> {
    protected final TModel animatableModel;
    protected final TAnimatable animatable;
    private boolean initialize = false;

    @Nullable
    private Future<AnimationEvent<TAnimatable>> task;

    public GeoInstance(TModel animatableModel, TAnimatable animatable, boolean asyncUpdate) {
        this.animatableModel = animatableModel;
        this.animatable = animatable;
        if(asyncUpdate) {
            AnimationParallelTicker.register(this);
        }
    }

    public void beginAsyncUpdate(final float partialTicks) {
        waitForAsyncUpdate();
        task = ThreadTools.submit(() -> performUpdate(partialTicks));
    }

    public AnimationEvent<TAnimatable> waitForAsyncUpdate() {
        if(task != null) {
            AnimationEvent<TAnimatable> result = null;
            try {
                result = task.get();
            } catch(InterruptedException ignored) {
            } catch(Exception e) {
                YesSteveModel.LOGGER.error("Error updating animation.", e);
            }
            task = null;
            return result;
        }
        return null;
    }

    public AnimationEvent<TAnimatable> waitOrUpdate(float partialTicks) {
        if(task != null) {
            return waitForAsyncUpdate();
        } else {
            return performUpdate(partialTicks);
        }
    }

    public AnimationEvent<TAnimatable> syncUpdate(float partialTicks) {
        waitForAsyncUpdate();
        return performUpdate(partialTicks);
    }

    @Nullable
    protected AnimationEvent<TAnimatable> performUpdate(float partialTicks) {
        if(!animatableModel.updateCurrentModel(animatable)) {
            return null;
        }
        final Entity entity = animatable.getEntity();
        final LivingEntity livingEntity = entity instanceof LivingEntity ? (LivingEntity)entity : null;

        boolean shouldSit = entity.isPassenger() && (entity.getVehicle() != null && entity.getVehicle().shouldRiderSit());
        float limbSwingAmount = 0;
        float limbSwing = 0;

        if (!shouldSit && entity.isAlive() && livingEntity != null) {
            limbSwingAmount = livingEntity.walkAnimation.speed(partialTicks);
            limbSwing = livingEntity.walkAnimation.position(partialTicks);
            if (livingEntity.isBaby()) {
                limbSwing *= 3.0F;
            }
        }

        EntityModelData entityModelData = new EntityModelData();
        entityModelData.isSitting = shouldSit;

        float lerpBodyRot = 0;
        float lerpHeadRot = 0;
        float netHeadYaw = 0;

        if(livingEntity != null) {
            entityModelData.isChild = livingEntity.isBaby();
            lerpBodyRot = Mth.rotLerp(partialTicks, livingEntity.yBodyRotO, livingEntity.yBodyRot);
            lerpHeadRot = Mth.rotLerp(partialTicks, livingEntity.yHeadRotO, livingEntity.yHeadRot);
            netHeadYaw = lerpHeadRot - lerpBodyRot;
        }

        if (shouldSit && entity.getVehicle() instanceof LivingEntity) {
            LivingEntity vehicle = (LivingEntity) entity.getVehicle();
            lerpBodyRot = Mth.rotLerp(partialTicks, vehicle.yBodyRotO, vehicle.yBodyRot);
            netHeadYaw = lerpHeadRot - lerpBodyRot;
            float clampedHeadYaw = Mth.clamp(Mth.wrapDegrees(netHeadYaw), -85, 85);
            lerpBodyRot = lerpHeadRot - clampedHeadYaw;
            if (clampedHeadYaw * clampedHeadYaw > 2500f) {
                lerpBodyRot += clampedHeadYaw * 0.2f;
            }
            netHeadYaw = lerpHeadRot - lerpBodyRot;
        }

        entityModelData.rawHeadPitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        entityModelData.headPitch = -entityModelData.rawHeadPitch;
        entityModelData.rawNetHeadYaw = netHeadYaw;
        entityModelData.netHeadYaw = -Mth.clamp(Mth.wrapDegrees(netHeadYaw), -85, 85);
        entityModelData.lerpBodyRot = lerpBodyRot;
        entityModelData.lerpedAge = entity.tickCount + partialTicks;

        AnimationEvent<TAnimatable> event = new AnimationEvent<>(animatable, limbSwing, limbSwingAmount, partialTicks, (limbSwingAmount <= -getSwingMotionAniMathHelperreshold() || limbSwingAmount <= getSwingMotionAniMathHelperreshold()), Collections.singletonList(entityModelData));
        AnimationContext<?> ctx = new AnimationContext<>(entity, this, event, entityModelData);
        ctx.setDebugSource(getDebugSource());
        animatableModel.setCustomAnimations(animatable, ctx, event);
        return event;
    }

    protected void setInitialized() {
        this.initialize = true;
    }

    public boolean isInitialized() {
        return this.initialize;
    }

    @Nullable
    public DebugSource getDebugSource() {
        return null;
    }

    public boolean canUpdateAsync() {
        return false;
    }

    private float getSwingMotionAniMathHelperreshold() {
        return 0.15f;
    }

    public TAnimatable getAnimatable() {
        return animatable;
    }

    public TModel getAnimatableModel() {
        return animatableModel;
    }

    @SuppressWarnings("all")
    public boolean isActive() {
        return !animatable.getEntity().isRemoved();
    }

    public String getModelId() {
        return this.animatableModel.getModelLocation(animatable);
    }

    public ResourceLocation getTextureLocation() {
        return this.animatableModel.getTextureLocation(animatable);
    }

    public abstract boolean isModelPresent();

    public abstract float getWidthScale();

    public abstract float getHeightScale();
}
