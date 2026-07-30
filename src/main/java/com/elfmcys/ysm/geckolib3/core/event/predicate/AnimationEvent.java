package com.elfmcys.ysm.geckolib3.core.event.predicate;

import com.elfmcys.ysm.geckolib3.core.controller.CodedAnimationController;
import com.elfmcys.ysm.geckolib3.model.AnimatableEntity;
import com.elfmcys.ysm.geckolib3.model.provider.data.EntityModelData;
import org.jetbrains.annotations.NotNull;

public class AnimationEvent<T extends AnimatableEntity<?>> {
    private final T animatable;
    private final float limbSwing;
    private final float limbSwingAmount;
    private final int entityTickCount;
    private final float requestedPartialTick;
    private final float partialTick;
    private final boolean isMoving;
    private final boolean renderingInLevelExclusive;
    public float renderTicks;
    private final EntityModelData extraData;
    protected CodedAnimationController<T> codedController;

    public AnimationEvent(T animatable,
                          float limbSwing, float limbSwingAmount,
                          int entityTickCount, float requestedPartialTick, float partialTick,
                          boolean isMoving,
                          boolean renderingInLevelExclusive,
                          @NotNull EntityModelData extraData) {
        this.animatable = animatable;
        this.limbSwing = limbSwing;
        this.limbSwingAmount = limbSwingAmount;
        this.entityTickCount = entityTickCount;
        this.requestedPartialTick = requestedPartialTick;
        this.partialTick = partialTick;
        this.renderTicks = entityTickCount + partialTick;
        this.isMoving = isMoving;
        this.renderingInLevelExclusive = renderingInLevelExclusive;
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

    public float getRequestedPartialTick() {
        return requestedPartialTick;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public boolean isRenderingInLevelExclusive() {
        return renderingInLevelExclusive;
    }

    public CodedAnimationController<T> getCodedController() {
        return codedController;
    }

    public void setCodedAnimationController(CodedAnimationController<T> controller) {
        this.codedController = controller;
    }

    @NotNull
    public EntityModelData getExtraData() {
        return extraData;
    }
}
