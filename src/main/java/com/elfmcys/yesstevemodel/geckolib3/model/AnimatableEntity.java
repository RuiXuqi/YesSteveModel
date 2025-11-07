package com.elfmcys.yesstevemodel.geckolib3.model;

import com.elfmcys.yesstevemodel.client.animation.molang.PhysicsManager;
import com.elfmcys.yesstevemodel.client.entity.IPreviewEntity;
import com.elfmcys.yesstevemodel.client.event.ClientTickEvent;
import com.elfmcys.yesstevemodel.client.sound.stream.AudioStreamProvider;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimationState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.manager.AnimationData;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IForeignVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.AnimationProcessor;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.core.util.RateLimiter;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.util.RenderUtil;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class AnimatableEntity<TEntity extends Entity> {
    private final AnimationData manager = new AnimationData();
    private final AnimationProcessor<TEntity> animationProcessor;
    private final RateLimiter rateLimiter;
    private final EntityStateTracker<TEntity> stateTracker;
    protected final PhysicsManager physicsManager;

    protected final TEntity entity;
    private GeoModelState currentModel;
    private Int2ReferenceMap<List<IValue>> eventHandlers;

    // 这两个变量不跟随动画一起更新，所以不能放进 stateTracker
    protected float lastFrameTime = -1;
    protected boolean lastMutableRender;
    protected boolean currentFrameTicked;
    protected boolean currentFrameShouldTick;

    protected boolean lastFrameRendered = true;
    protected boolean currentFrameRendered = false;
    protected boolean lastFrameUpdated;
    protected float seekTime;
    protected boolean initialize = false;

    /**
     * 存储 Coded 动画控制器的动画播放状态，用于一些 molang 判断
     */
    protected Map<String, AnimationState> codedAnimationStates = Maps.newHashMap();

    protected AnimatableEntity(TEntity entity) {
        this.entity = entity;
        this.animationProcessor = new AnimationProcessor<>(this);
        this.rateLimiter = new RateLimiter();
        this.stateTracker = createStateTracker(entity);
        this.physicsManager = new PhysicsManager();
        this.rateLimiter.setLimit(getFrameRateLimit());
    }

    protected void reset() {
        clearGeoModel();
        rateLimiter.reset();
        manager.reset();
        stateTracker.reset();
        lastFrameTime = -1;
        lastMutableRender = false;
        currentFrameTicked = false;
        currentFrameShouldTick = false;
        lastFrameRendered = true;
        currentFrameRendered = false;
        lastFrameUpdated = false;
        seekTime = 0;
        initialize = false;
        codedAnimationStates.clear();
    }

    protected EntityStateTracker<TEntity> createStateTracker(TEntity entity) {
        return new EntityStateTracker<>(entity);
    }

    public EntityStateTracker<TEntity> getStateTracker() {
        return stateTracker;
    }

    public float getSeekTime() {
        return seekTime;
    }

    public void addAnimationController(IAnimationController<? extends AnimatableEntity<TEntity>> value) {
        this.manager.addAnimationController(value);
    }

    public AnimationData getAnimationData() {
        return manager;
    }

    public abstract ResourceLocation getTextureLocation();

    public abstract boolean isModelPresent();

    public abstract float getWidthScale();

    public abstract float getHeightScale();

    @Nullable
    public abstract Animation getAnimation(String name);

    @Nullable
    public IValue getUserFunction(int name) {
        return null;
    }

    public Optional<AudioStreamProvider> getSoundStream(String name) {
        return Optional.empty();
    }

    @Nullable
    public final List<IValue> getEventHandler(int name) {
        return this.eventHandlers.get(name);
    }

    public PhysicsManager getPhysicsManager() {
        return physicsManager;
    }

    @Nullable
    public AnimationControllerData getAnimationControllerData(String animationControllerName) {
        return null;
    }

    public int getTextureIndex() {
        return 0;
    }

    protected float getSwingMotionAniMathHelperreshold() {
        return 0.15f;
    }

    /**
     * 更新动画之前调用，
     * 如果由于频率限制、renderTick 倒退等原因导致动画不更新，则不会调用
     */
    protected void preAnimationSetup(float seekTime, boolean shouldTick) {
    }

    /**
     * 更新动画之后调用，
     * 如果由于频率限制、renderTick 倒退等原因导致动画不更新，则不会调用
     */
    protected void postAnimationSetup(float seekTime, boolean shouldTick) {
    }

    public final TEntity getEntity() {
        return entity;
    }

    public boolean isFakePlayer() {
        return false;
    }

    @Nullable
    public IBone getBone(int boneName) {
        return animationProcessor.getBone(boneName);
    }

    protected boolean allowEmitting() {
        return true;
    }

    public int getFrameRateLimit() {
        var localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null && localPlayer != entity) {
            var localPos = localPlayer.position();
            if (localPos.x != 0 || localPos.y != 0 || localPos.z != 0) {
                // 未渲染：屏幕外、太远、被 EntityCulling 剔除
                if (!lastFrameRendered) {
                    return 10;
                }

                var distance = localPlayer.distanceTo(entity);
                // 超远距离：原版超过这个距离会跳过渲染
                if (distance > 64) {
                    return 30;
                }
                // 一般远距离
                if (distance > 40) {
                    return 60;
                }
            }
        }
        return ClientTickEvent.getRefreshRate();
    }

    public final @Nullable AnimationEvent<?> updateAnimation(float partialTicks) {
        return updateAnimation(partialTicks, RenderUtil.isRenderingLevelExclusive());
    }

    @Nullable
    public AnimationEvent<?> updateAnimation(float partialTicks, boolean renderingInLevelExclusive) {
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

        AnimationEvent<AnimatableEntity<TEntity>> event = new AnimationEvent<>(this,
                limbSwing, limbSwingAmount,
                entityTickCount, partialTicks, realPartialTicks,
                (limbSwingAmount <= -getSwingMotionAniMathHelperreshold() || limbSwingAmount <= getSwingMotionAniMathHelperreshold()),
                renderingInLevelExclusive,
                entityModelData);
        MolangContext<?> ctx = new MolangContext<>(entity, this, event, entityModelData);
        ctx.setDebugSource(getDebugSource());
        this.tickAnimation(ctx, event);
        return event;
    }

    protected void tickAnimation(MolangContext<?> ctx, @NotNull AnimationEvent<AnimatableEntity<TEntity>> animationEvent) {
        var frameTime = animationEvent.renderTicks;
        var mutableRender = !isImmutableRender(animationEvent);

        if (frameTime > lastFrameTime) {
            currentFrameTicked = false;
            currentFrameShouldTick = false;
            lastFrameTime = frameTime;
            rateLimiter.setLimit(getFrameRateLimit());
            lastFrameRendered = currentFrameRendered;
            currentFrameRendered = false;
        } else {
            // 目前不允许倒退，可能会影响 replay 的回放
            frameTime = lastFrameTime;
        }

        if (manager.startTick == -1) {
            manager.startTick = frameTime;
        } else {
            float currentTick = frameTime - manager.startTick;
            float deltaTicks = currentTick - manager.lastTick;
            if (deltaTicks > 0f) {
                manager.lastTick = currentTick;
                this.seekTime += deltaTicks;
            }
        }
        animationEvent.renderTicks = this.seekTime;

        if (!animationProcessor.isModelEmpty()) {
            currentFrameShouldTick |= rateLimiter.request(seekTime / 20);
            var shouldUpdate = (currentFrameShouldTick && !currentFrameTicked) || lastMutableRender || mutableRender;
            var shouldTick = !mutableRender && currentFrameShouldTick && !currentFrameTicked;
            recoverLastCodedAnimation(lastFrameUpdated);
            if (shouldUpdate) {
                if (shouldTick) {
                    currentFrameTicked = true;
                    stateTracker.update(animationEvent.getEntityTickCount(), this.seekTime, animationEvent.getPartialTick());
                }
                getPhysicsManager().update(this.seekTime);
                preAnimationSetup(this.seekTime, shouldTick);
                getAnimationProcessor().tickAnimation(animationEvent, ctx, shouldTick, allowEmitting());
                postAnimationSetup(this.seekTime, shouldTick);
                lastMutableRender = mutableRender;
            }
            codeAnimation(animationEvent, shouldUpdate);
            lastFrameUpdated = shouldUpdate;
        }
    }

    protected void codeAnimation(AnimationEvent<? extends AnimatableEntity<TEntity>> animationEvent, boolean shouldUpdate) {
    }

    protected void recoverLastCodedAnimation(boolean lastFrameUpdated) {
    }

    public AnimationProcessor<TEntity> getAnimationProcessor() {
        return this.animationProcessor;
    }

    /**
     * 设置模型
     */
    protected void loadGeoModel(@NotNull GeoModel model, Int2ReferenceMap<List<IValue>> eventHandlers) {
        this.currentModel = new GeoModelState(model);
        this.eventHandlers = eventHandlers;
        this.animationProcessor.loadModel(currentModel.boneMap(), eventHandlers);
        onLoadGeoModel(this.currentModel);
        this.currentFrameTicked = false;
        this.currentFrameShouldTick = true;
        this.rateLimiter.reset();
        this.lastMutableRender = false;
    }

    protected void clearGeoModel() {
        this.currentModel = null;
        this.eventHandlers = null;
        this.animationProcessor.clearModel();
        this.physicsManager.reset();
    }

    public void reloadGeoModel() {
        if (this.currentModel != null) {
            this.currentModel = new GeoModelState(this.currentModel.model());
            this.animationProcessor.loadModel(currentModel.boneMap(), eventHandlers);
            onLoadGeoModel(this.currentModel);
        }
    }

    /**
     * 获取当前正在使用的模型
     */
    @Nullable
    public final GeoModelState getLoadedGeoModel() {
        return currentModel;
    }

    /**
     * 更新当前使用的模型后调用
     */
    protected void onLoadGeoModel(GeoModelState model) {
        physicsManager.reset();
    }

    /**
     * 渲染期间是否会保持实体属性不变
     */
    protected boolean isImmutableRender(AnimationEvent<?> animEvent) {
        return true;
    }

    public void countRender() {
        currentFrameRendered = true;
    }

    public void executeMolangExp(IValue value, boolean allowEmitting, boolean pre, @Nullable Consumer<String> resultConsumer) {
        animationProcessor.enqueueMolangTask(value, allowEmitting, pre, resultConsumer);
    }

    public IForeignVariableStorage getPublicVariableStorage() {
        return this.animationProcessor.getPublicVariableStorage();
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

    @SuppressWarnings("resource")
    public boolean isActive() {
        return Minecraft.getInstance().level == entity.level() && !entity.isRemoved();
    }

    public void setCodedAnimationStates(String controllerName, AnimationState state) {
        this.codedAnimationStates.put(controllerName, state);
    }

    public AnimationState getCodedAnimationStates(String controllerName) {
        return this.codedAnimationStates.getOrDefault(controllerName, AnimationState.IDLE);
    }
}
