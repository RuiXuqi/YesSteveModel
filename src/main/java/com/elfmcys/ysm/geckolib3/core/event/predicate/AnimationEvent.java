package com.elfmcys.ysm.geckolib3.core.event.predicate;

import com.elfmcys.ysm.geckolib3.core.controller.CodedAnimationController;
import com.elfmcys.ysm.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.ysm.geckolib3.geo.RenderContext;
import com.elfmcys.ysm.geckolib3.model.AnimatableEntity;
import com.elfmcys.ysm.geckolib3.model.provider.data.EntityModelData;
import org.jetbrains.annotations.NotNull;

public class AnimationEvent<T extends AnimatableEntity<?>> {
    private final T animatable;
    private final int entityTickCount;
    private final float requestedPartialTick;
    private final float partialTick;
    private final RenderContext renderContext;
    public float renderTicks;
    private final EntityModelData extraData;
    private final DebugSource debugSource;
    protected CodedAnimationController<T> codedController;

    public AnimationEvent(T animatable,
                          int entityTickCount, float requestedPartialTick, float partialTick,
                          RenderContext renderContext,
                          @NotNull EntityModelData extraData,
                          DebugSource debugSource) {
        this.animatable = animatable;
        this.entityTickCount = entityTickCount;
        this.requestedPartialTick = requestedPartialTick;
        this.partialTick = partialTick;
        this.renderTicks = entityTickCount + partialTick;
        this.renderContext = renderContext;
        this.extraData = extraData;
        this.debugSource = debugSource;
    }

    public float getRenderTicks() {
        return renderTicks;
    }

    public T getAnimatableEntity() {
        return animatable;
    }

    public float getLimbSwing() {
        return extraData.limbSwing;
    }

    public float getLimbSwingAmount() {
        return extraData.limbSwingAmount;
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
        return extraData.isMoving;
    }

    public boolean isRenderingInLevelExclusive() {
        return renderContext.level();
    }

    public RenderContext getRenderContext() {
        return renderContext;
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

    public DebugSource getDebugSource() {
        return debugSource;
    }
}
