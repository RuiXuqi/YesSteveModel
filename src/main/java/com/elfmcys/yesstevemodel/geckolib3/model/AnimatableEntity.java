package com.elfmcys.yesstevemodel.geckolib3.model;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.AnimationParallelTicker;
import com.elfmcys.yesstevemodel.client.entity.IPreviewEntity;
import com.elfmcys.yesstevemodel.client.event.ClientTickEvent;
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
import com.elfmcys.yesstevemodel.util.ThreadTools;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@SuppressWarnings("unchecked,rawtypes")
public abstract class AnimatableEntity<TEntity extends Entity> {
    private final AnimationData manager = new AnimationData();
    private final AnimationProcessor animationProcessor;
    private final RateLimiter rateLimiter;
    private final EntityStateTracker<TEntity> entityStateTracker;

    protected final TEntity entity;
    private GeoModelState currentModel;

    private float seekTime;
    private boolean initialize = false;

    @Nullable
    private Future<AnimationEvent<?>> task;

    protected AnimatableEntity(TEntity entity, boolean asyncUpdate) {
        this.entity = entity;
        this.animationProcessor = new AnimationProcessor(this);
        this.rateLimiter = new RateLimiter(Minecraft.getInstance().getWindow().getRefreshRate());
        this.entityStateTracker = createStateTracker(entity);
        if (asyncUpdate) {
            AnimationParallelTicker.register(this);
        }
    }

    protected EntityStateTracker<TEntity> createStateTracker(TEntity entity) {
        return new EntityStateTracker(entity);
    }

    public EntityStateTracker<TEntity> getStateTracker() {
        return entityStateTracker;
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

    /**
     * 获取当前选中的模型，
     * 在更新前未必等于 currentModel
     */
    public abstract GeoModel getModel();

    public abstract boolean isModelPresent();

    public abstract float getWidthScale();

    public abstract float getHeightScale();

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

    protected boolean updateAnimation(MolangContext<?> ctx, @NotNull AnimationEvent<?> animationEvent) {
        var frameTime = animationEvent.getEntityTickCount() + animationEvent.getPartialTick();

        if (manager.startTick == -1) {
            manager.startTick = frameTime;
        } else {
            float currentTick = frameTime - manager.startTick;
            float deltaTicks = currentTick - manager.lastTick;
            if (deltaTicks < 0f) {  // 目前不允许倒退，可能会影响 replay 的回放
                return false;
            }
            manager.lastTick = currentTick;
            this.seekTime += deltaTicks;
        }

        boolean forceUpdate = this.shouldForceUpdate();
        animationEvent.renderTicks = this.seekTime;

        if (!getAnimationProcessor().isModelEmpty()) {
            var shouldUpdate = rateLimiter.request(seekTime / 20);
            if (forceUpdate || shouldUpdate) {
                entityStateTracker.update(animationEvent.getEntityTickCount(), this.seekTime, animationEvent.getPartialTick());
                preAnimationSetup(this.seekTime);
                getAnimationProcessor().tickAnimation(shouldUpdate, animationEvent, ctx);
                return true;
            }
        }
        return false;
    }

    public AnimationProcessor getAnimationProcessor() {
        return this.animationProcessor;
    }

    /**
     * 检查选择的模型是否有更新并尝试更新，成功更新返回 true，未更新返回 false
     */
    public boolean updateCurrentModel(boolean force) {
        GeoModel model = getModel();
        if (model == null) {
            if (this.currentModel != null) {
                this.currentModel = null;
                return true;
            }
            return false;
        }
        if (force || this.currentModel == null || model != this.currentModel.model()) {
            this.currentModel = new GeoModelState(model);
            this.animationProcessor.registerModelBones(currentModel.boneMap());
            setupModel(this.currentModel);
            return true;
        }
        return false;
    }

    /**
     * 获取当前正在使用的模型
     */
    public GeoModelState getCurrentModel() {
        return currentModel;
    }

    /**
     * 更新当前使用的模型后调用
     */
    protected void setupModel(GeoModelState model) {
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
        this.updateCurrentModel(false);
        if (this.currentModel == null) {
            return null;
        }
        final Entity entity = this.entity;
        final LivingEntity livingEntity = entity instanceof LivingEntity ? (LivingEntity) entity : null;
        int entityTickCount = this instanceof IPreviewEntity ? ClientTickEvent.getTickCount() : entity.tickCount;
        float realPartialTicks = partialTicks != 1f ? partialTicks : Minecraft.getInstance().getFrameTime();

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
        entityModelData.lerpedAge = entityTickCount + partialTicks;

        AnimationEvent<?> event = new AnimationEvent<>(this, limbSwing, limbSwingAmount, entityTickCount, realPartialTicks, (limbSwingAmount <= -getSwingMotionAniMathHelperreshold() || limbSwingAmount <= getSwingMotionAniMathHelperreshold()), entityModelData);
        MolangContext<?> ctx = new MolangContext<>(entity, this, event, entityModelData);
        ctx.setDebugSource(getDebugSource());
        this.updateAnimation(ctx, event);
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
