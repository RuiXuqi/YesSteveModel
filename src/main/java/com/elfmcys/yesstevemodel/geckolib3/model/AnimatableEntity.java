package com.elfmcys.yesstevemodel.geckolib3.model;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.AnimationParallelTicker;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.manager.AnimationData;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IForeignVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.AnimationProcessor;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.DebugInfo;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.core.util.RateLimiter;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.elfmcys.yesstevemodel.util.ThreadTools;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@SuppressWarnings("unchecked,rawtypes")
public abstract class AnimatableEntity<TEntity extends Entity> {
    private final AnimationData manager = new AnimationData();
    private final AnimationProcessor animationProcessor;
    private final RateLimiter rateLimiter;

    protected TEntity entity;
    private GeoModelState currentModel;

    private float seekTime;
    private float lastGameTickTime;
    private boolean initialize = false;

    private Vec3 lastPosition;
    private Vec3 positionDelta = Vec3.ZERO;
    /**
     * ms
     */
    protected float lastFrameTime;

    @Nullable
    private Future<AnimationEvent<?>> task;

    protected AnimatableEntity(TEntity entity, boolean asyncUpdate) {
        this.entity = entity;
        this.animationProcessor = new AnimationProcessor(this);
        this.rateLimiter = new RateLimiter(Minecraft.getInstance().getWindow().getRefreshRate());
        if (asyncUpdate) {
            AnimationParallelTicker.register(this);
        }
    }

    public float getSeekTime() {
        return seekTime;
    }

    public void addAnimationController(IAnimationController value) {
        this.manager.addAnimationController(value);
    }

    public AnimationData getAnimationData() {
        return manager;
    }

    public abstract String getModelId();

    public abstract ResourceLocation getTextureLocation();

    public abstract GeoModel getModel();

    public abstract boolean isModelPresent();

    public abstract float getWidthScale();

    public abstract float getHeightScale();

    public boolean hasPreviewAnimation() {
        // 是否有预览动画功能，目前仅有玩家支持此功能
        return false;
    }

    @Nullable
    public abstract Animation getAnimation(String name);

    @Nullable
    public IValue getUserFunction(int name) {
        return null;
    }

    @Nullable
    public List<IValue> getEventHandler(int name) {
        return null;
    }

    @Nullable
    public GeoAnimationController getAnimationControllerData(String animationControllerName) {
        return null;
    }

    public int getTextureIndex() {
        return 0;
    }

    protected float getSwingMotionAniMathHelperreshold() {
        return 0.15f;
    }

    protected void preAnimationSetup(float seekTime) {
    }

    public final TEntity getEntity() {
        return entity;
    }

    @Nullable
    public IBone getBone(String boneName) {
        return animationProcessor.getBone(boneName);
    }

    public boolean setCustomAnimations(MolangContext<?> ctx, @NotNull AnimationEvent<?> animationEvent) {
        Minecraft mc = Minecraft.getInstance();

        boolean forceUpdate = this.shouldForceUpdate();
        float currentTick = getCurrentTick();

        if (manager.startTick == -1) {
            manager.startTick = currentTick;
        } else {
            manager.tick = currentTick - manager.startTick;
            if (!mc.isPaused() || manager.shouldPlayWhilePaused) {
                float deltaTicks = manager.tick - this.lastGameTickTime;
                this.seekTime += deltaTicks;
            }
            this.lastGameTickTime = manager.tick;
        }

        animationEvent.animationTick = this.seekTime;
        if (!getAnimationProcessor().isModelRendererEmpty()) {
            var shouldUpdate = rateLimiter.request((float) (seekTime / 20));
            if (forceUpdate || shouldUpdate) {
                float currentFrameTime = (float) getCurrentTick() * 50;
                if (currentFrameTime > lastFrameTime && lastFrameTime != 0) {
                    updateFrameData(currentFrameTime, lastFrameTime, animationEvent.getPartialTick());
                }

                preAnimationSetup(this.seekTime);
                getAnimationProcessor().tickAnimation(this.seekTime, shouldUpdate, animationEvent, ctx);

                lastFrameTime = currentFrameTime;
                return true;
            }
        }
        return false;
    }

    protected void updateFrameData(float currentFrameTime, float lastFrameTime, float partialTicks) {
        updatePositionDelta(partialTicks);
    }

    private void updatePositionDelta(float partialTicks) {
        var cur = new Vec3(Mth.lerp(partialTicks, entity.xo, entity.getX()),
                Mth.lerp(partialTicks, entity.yo, entity.getY()),
                Mth.lerp(partialTicks, entity.zo, entity.getZ()));
        if (lastPosition != null) {
            positionDelta = cur.subtract(lastPosition);
        }
        lastPosition = cur;
    }

    public Vec3 getPositionDelta() {
        return positionDelta;
    }

    public AnimationProcessor getAnimationProcessor() {
        return this.animationProcessor;
    }

    public boolean updateCurrentModel(boolean force) {
        GeoModel model = getModel();
        if (model == null) {
            this.currentModel = null;
            return false;
        }
        if (force || this.currentModel == null || model != this.currentModel.model()) {
            this.currentModel = new GeoModelState(model);
            this.animationProcessor.registerModelRenderer(currentModel.boneMap());
            setupModel(this.currentModel);
        }
        return true;
    }

    public GeoModelState getCurrentModel() {
        return currentModel;
    }

    protected void setupModel(GeoModelState model) {
    }

    public float getCurrentTick() {
        return RenderUtils.getRenderTickTime();
    }

    public boolean shouldForceUpdate() {
        return false;
    }

    public DebugInfo getDebugInfo() {
        return animationProcessor.getDebugInfo();
    }

    public void executeMolangExp(IValue value, boolean allowEmitting, boolean pre, @Nullable Consumer<String> resultConsumer) {
        animationProcessor.enqueueMolangTask(value, allowEmitting, pre, resultConsumer);
    }

    public IForeignVariableStorage getPublicVariableStorage() {
        return this.animationProcessor.getPublicVariableStorage();
    }

    public void beginAsyncUpdate(final float partialTicks) {
        waitForAsyncUpdate();
        task = ThreadTools.submit(() -> performUpdate(partialTicks));
    }

    public AnimationEvent<?> waitForAsyncUpdate() {
        if (task != null) {
            AnimationEvent<?> result = null;
            try {
                result = task.get();
            } catch (InterruptedException ignored) {
            } catch (Throwable e) {
                YesSteveModel.LOGGER.error("Error updating animation.", e);
            }
            task = null;
            return result;
        }
        return null;
    }

    public AnimationEvent<?> waitOrUpdate(float partialTicks) {
        if (task != null) {
            return waitForAsyncUpdate();
        } else {
            return performUpdate(partialTicks);
        }
    }

    public AnimationEvent<?> syncUpdate(float partialTicks) {
        waitForAsyncUpdate();
        return performUpdate(partialTicks);
    }

    @Nullable
    protected AnimationEvent<?> performUpdate(float partialTicks) {
        if (!this.updateCurrentModel(false)) {
            return null;
        }
        final Entity entity = this.entity;
        final LivingEntity livingEntity = entity instanceof LivingEntity ? (LivingEntity) entity : null;

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

        if (livingEntity != null) {
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

        AnimationEvent<?> event = new AnimationEvent<>(this, limbSwing, limbSwingAmount, partialTicks, (limbSwingAmount <= -getSwingMotionAniMathHelperreshold() || limbSwingAmount <= getSwingMotionAniMathHelperreshold()), Collections.singletonList(entityModelData));
        MolangContext<?> ctx = new MolangContext<>(entity, this, event, entityModelData);
        ctx.setDebugSource(getDebugSource());
        this.setCustomAnimations(ctx, event);
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

    public boolean isActive() {
        return Minecraft.getInstance().level == entity.level() && !entity.isRemoved();
    }

    public boolean isTacGunAnimationNeedReload() {
        return false;
    }

    public void setTacGunAnimationNeedReload(boolean needReload) {
    }
}
