package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.AnimationParallelTicker;
import com.elfmcys.yesstevemodel.client.animation.debug.CustomDebugSource;
import com.elfmcys.yesstevemodel.client.animation.molang.PhysicsManager;
import com.elfmcys.yesstevemodel.client.compat.IrisCompat;
import com.elfmcys.yesstevemodel.client.gui.overlay.DebugAnimationScreen;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.client.sound.data.SoundFormat;
import com.elfmcys.yesstevemodel.client.sound.data.ModelSoundHolder;
import com.elfmcys.yesstevemodel.client.sound.data.SoundDataManager;
import com.elfmcys.yesstevemodel.client.sound.stream.AudioStreamProvider;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.DebugInfo;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.elfmcys.yesstevemodel.util.RenderUtil;
import com.elfmcys.yesstevemodel.util.ThreadTools;
import com.elfmcys.yesstevemodel.util.UnsafeUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.Future;

/**
 * 自动管理当前 model id 和 model container，并在找不到指定模型时 fallback 到默认模型
 */
public abstract class CustomEntity<T extends Entity> extends AnimatableEntity<T> {
    private String modelId = ModelIdUtil.DEFAULT_MODEL_ID;
    private ClientModel currentModelContainer;
    private ResourceHolder resourceHolder;
    private boolean modelFallback;
    private int lastCheckUpdateTime;
    @Nullable
    private PhysicsManager alterPhysicsManager;
    @Nullable
    private DebugInfo debugInfo;

    @Nullable
    private Future<AnimationEvent<?>> asyncTask;

    protected CustomEntity(T entity, boolean asyncUpdate) {
        super(entity);
        if (asyncUpdate) {
            AnimationParallelTicker.add(this);
        }
    }

    @Override
    protected void reset() {
        modelId = ModelIdUtil.DEFAULT_MODEL_ID;
        currentModelContainer = null;
        resourceHolder = null;
        modelFallback = false;
        lastCheckUpdateTime = 0;
        alterPhysicsManager = null;
        super.reset();
    }

    @Override
    public PhysicsManager getPhysicsManager() {
        if (RenderUtil.isRenderingLevel() || RenderUtil.isRenderingInPaperDoll()) {
            return physicsManager;
        } else {
            if (alterPhysicsManager == null) {
                alterPhysicsManager = new PhysicsManager();
            }
            return alterPhysicsManager;
        }
    }

    public void setDebugInfo(@Nullable DebugInfo debugInfo) {
        this.debugInfo = debugInfo;
    }

    @Override
    protected void preAnimationSetup(float seekTime, boolean shouldTick) {
        super.preAnimationSetup(seekTime, shouldTick);
        // 更新调试信息
        if (debugInfo != null) {
            var processor = getAnimationProcessor();
            processor.enqueueMolangTask(evaluator -> {
                debugInfo.evaluatePre(evaluator);
                return null;
            }, false, true, null);
            processor.enqueueMolangTask(evaluator -> {
                debugInfo.evaluatePost(evaluator);
                return null;
            }, false, false, null);
        }
    }

    @Override
    protected void onLoadGeoModel(GeoModelState model) {
        super.onLoadGeoModel(model);
        if (alterPhysicsManager != null) {
            alterPhysicsManager.reset();
        }
    }

    public void checkModelUpdate() {
        if (lastCheckUpdateTime < entity.tickCount) {
            checkModelContainerUpdate();
            lastCheckUpdateTime = entity.tickCount;
        }
    }

    public final ClientModel getModelContainer() {
        return currentModelContainer;
    }

    protected final void updateModelId(String modelId) {
        this.modelId = modelId;
        checkModelContainerUpdate();
    }

    private void checkModelContainerUpdate() {
        ClientModelManager.getModel(modelId).ifPresentOrElse(model -> {
            if (resourceHolder == null || resourceHolder.fallback || model != resourceHolder.model) {
                resourceHolder = createResourceHolder(model, false);
            }
        }, () -> {
            var defaultModel = ClientModelManager.getDefaultModel();
            if (resourceHolder == null || !resourceHolder.fallback || defaultModel != resourceHolder.model) {
                resourceHolder = createResourceHolder(defaultModel, true);
            }
        });

        if (resourceHolder != null) {
            if ((resourceHolder.model != currentModelContainer || resourceHolder.fallback != modelFallback) && resourceHolder.isLoaded()) {
                currentModelContainer = resourceHolder.model;
                modelFallback = resourceHolder.fallback;
                onLoadModelContainer(currentModelContainer);
                loadGeoModel(getYsmGeoModel(), currentModelContainer.assets().eventHandlers());
            }
        } else if (currentModelContainer != null) {
            onClearModelContainer();
            currentModelContainer = null;
        }
    }

    protected void onClearModelContainer() {
        clearGeoModel();
    }

    @Nullable
    protected abstract ResourceHolder createResourceHolder(ClientModel model, boolean isFallback);

    protected final ResourceHolder getResourceHolder() {
        return resourceHolder;
    }

    protected void onLoadModelContainer(ClientModel newModel) {
        resourceHolder.soundHolder = SoundDataManager.register(newModel);
    }

    // getGeoModel 跟女仆的 IGeoEntity 冲突了，所以叫这个
    protected abstract GeoModel getYsmGeoModel();

    public final String getModelId() {
        return modelId;
    }

    @Override
    public boolean isModelPresent() {
        return resourceHolder != null && !resourceHolder.fallback && resourceHolder.isLoaded();
    }

    @Override
    protected boolean isImmutableRender(AnimationEvent<?> animEvent) {
        // 已知场景内、iris 阴影不会修改实体参数；
        // FirstPersonMod 会隐藏头部、原版 inventory 会修改身体和头部旋转、纸娃娃可能会基于 molang 应用不同的效果
        return animEvent.isRenderingInLevelExclusive() || IrisCompat.isRenderingShadow();
    }

    @Override
    @Nullable
    public final IValue getUserFunction(int name) {
        return getModelContainer().assets().userFunctions().get(name);
    }

    @Override
    public Optional<AudioStreamProvider> getSoundStream(String name) {
        if (resourceHolder.soundHolder != null) {
            var soundData = getModelContainer().assets().sounds().get(name);
            if (soundData != null && soundData.soundFormat() != SoundFormat.UNDEFINED) {
                var holder = resourceHolder.soundHolder;
                return Optional.of(() -> holder.openStream(soundData));
            }
        }
        return Optional.empty();
    }

    @Override
    public DebugSource getDebugSource() {
        if (DebugAnimationScreen.isEnabled()) {
            return CustomDebugSource.INSTANCE;
        } else {
            return null;
        }
    }

    public void beginAsyncUpdate(final float partialTicks) {
        UnsafeUtil.getUnsafe().storeFence();
        asyncTask = ThreadTools.submit(() -> {
            try {
                // 异步更新的作用域固定，无须判断
                return super.updateAnimation(partialTicks, true);
            } finally {
                UnsafeUtil.getUnsafe().storeFence();
            }
        });
    }

    @Override
    public @Nullable AnimationEvent<?> updateAnimation(float partialTicks, boolean renderingInLevelExclusive) {
        RenderSystem.assertOnRenderThread();
        if (renderingInLevelExclusive) {
            if (asyncTask != null) {
                return waitForAsyncUpdate();
            }
        }
        waitForAsyncUpdate();
        return super.updateAnimation(partialTicks, renderingInLevelExclusive);
    }

    public AnimationEvent<?> waitForAsyncUpdate() {
        if (asyncTask != null) {
            AnimationEvent<?> result = null;
            try {
                result = asyncTask.get();
                UnsafeUtil.getUnsafe().loadFence();
            } catch (InterruptedException ignored) {
            } catch (Throwable e) {
                e.printStackTrace();
            }
            asyncTask = null;
            return result;
        }
        return null;
    }

    public boolean canUpdateAsync() {
        return true;
    }

    protected static class ResourceHolder {
        public final ClientModel model;
        public final boolean fallback;
        @Nullable
        public ModelSoundHolder soundHolder;

        protected ResourceHolder(ClientModel model, boolean fallback) {
            this.model = model;
            this.fallback = fallback;
        }

        public boolean isLoaded() {
            return true;
        }
    }
}
