package com.elfmcys.yesstevemodel.geckolib3.core.event.predicate;

import com.elfmcys.yesstevemodel.geckolib3.core.controller.CodedAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class AnimationEvent<T extends AnimatableEntity<?>> {
    private final T animatable;
    private final float limbSwing;
    private final float limbSwingAmount;
    private final int entityTickCount;
    private final float partialTick;
    private final boolean isMoving;
    public float renderTicks;
    @Nullable
    private final EntityModelData extraData;
    @Nullable
    protected CodedAnimationController<T> codedController;

    public AnimationEvent(T animatable, float limbSwing, float limbSwingAmount, int entityTickCount, float partialTick, boolean isMoving,
                          @Nullable EntityModelData extraData) {
        this.animatable = animatable;
        this.limbSwing = limbSwing;
        this.limbSwingAmount = limbSwingAmount;
        this.entityTickCount = entityTickCount;
        this.partialTick = partialTick;
        this.isMoving = isMoving;
        this.extraData = extraData;
    }

    public float getRenderTicks() {
        return renderTicks;
    }

    public T getAnimatableEntity() {
        return animatable;
    }

    public float getLimbSwing() {
        return limbSwing;
    }

    public float getLimbSwingAmount() {
        return limbSwingAmount;
    }

    public int getEntityTickCount() {
        return entityTickCount;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public boolean isMoving() {
        return isMoving;
    }

    @NotNull
    public CodedAnimationController<T> getCodedController() {
        Objects.requireNonNull(codedController);
        return codedController;
    }

    public void setCodedAnimationController(@NotNull CodedAnimationController<T> controller) {
        this.codedController = controller;
    }

    @Nullable
    public EntityModelData getExtraData() {
        return extraData;
    }
}
