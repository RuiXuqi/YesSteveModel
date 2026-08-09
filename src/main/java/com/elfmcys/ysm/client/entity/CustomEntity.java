package com.elfmcys.ysm.client.entity;

import com.elfmcys.ysm.client.animation.AnimationParallelTicker;
import com.elfmcys.ysm.client.animation.debug.CustomDebugSource;
import com.elfmcys.ysm.client.animation.molang.MolangEventWrapper;
import com.elfmcys.ysm.client.animation.molang.PhysicsManager;
import com.elfmcys.ysm.client.gui.overlay.DebugAnimationScreen;
import com.elfmcys.ysm.client.model.ModelRenderTarget;
import com.elfmcys.ysm.client.model.ClientModelService;
import com.elfmcys.ysm.client.model.ModelRenderTargetLease;
import com.elfmcys.ysm.client.sound.data.ModelSoundHolder;
import com.elfmcys.ysm.client.sound.stream.AudioStreamProvider;
import com.elfmcys.ysm.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.ysm.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.ysm.geckolib3.core.molang.value.IValue;
import com.elfmcys.ysm.geckolib3.core.processor.DebugInfo;
import com.elfmcys.ysm.geckolib3.geo.GeoRenderData;
import com.elfmcys.ysm.geckolib3.geo.RenderContext;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.ysm.geckolib3.model.AnimatableEntity;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.domain.RenderTargetIds;
import com.elfmcys.ysm.util.ThreadTools;
import com.elfmcys.ysm.util.UnsafeUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

/**
 * 自动管理当前 model id 和 model container，并在找不到指定模型时 fallback 到默认模型
 */
public abstract class CustomEntity<T extends Entity> extends AnimatableEntity<T> {
    private final EntityModelBinding modelBinding = new EntityModelBinding();
    private ModelRenderTarget currentModelRenderTarget;
    private boolean modelFallback;
    private int lastCheckUpdateTime;
    @Nullable
    private PhysicsManager alterPhysicsManager;
    @Nullable
    private DebugInfo debugInfo;
    @Nullable
    private List<IValue> deferHandler;

    @Nullable
    private Future<GeoRenderData> asyncTask;

    protected CustomEntity(T entity, boolean asyncUpdate) {
        super(entity);
        if (asyncUpdate) {
            AnimationParallelTicker.add(this);
        }
    }

    @Override
    public PhysicsManager getPhysicsManager(AnimationEvent<?> event) {
        var context = event.getRenderContext();
        if (context.immutable() || context.firstPersonMod() || context.paperDoll()) {
            return physicsManager;
        }
        if (alterPhysicsManager == null) {
            alterPhysicsManager = new PhysicsManager();
        }
        return alterPhysicsManager;
    }

    @Nullable
    public List<IValue> getMolangDeferHandler() {
        return deferHandler;
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

    public void checkModelUpdate() {
        if (lastCheckUpdateTime < entity.tickCount) {
            checkModelRenderTargetUpdate();
            lastCheckUpdateTime = entity.tickCount;
        }
    }

    public final ModelRenderTarget getModelRenderTarget() {
        return currentModelRenderTarget;
    }

    protected final void updateModelHash(Hash256 modelHash) {
        modelBinding.updateModelHash(modelHash);
        checkModelRenderTargetUpdate();
    }

    private void checkModelRenderTargetUpdate() {
        modelBinding.synchronize(
                requestedRenderTargetId(), requestedTextureName(), fallbackRenderTargetId(), this::createResourceHolder);
        var resourceHolder = modelBinding.resourceHolder();
        if (resourceHolder != null) {
            if ((resourceHolder.model != currentModelRenderTarget || resourceHolder.fallback != modelFallback) && resourceHolder.isLoaded()) {
                currentModelRenderTarget = resourceHolder.model;
                modelFallback = resourceHolder.fallback;
                onModelRenderTargetLoaded(currentModelRenderTarget);
                loadGeoModel(getYsmGeoModel(), currentModelRenderTarget.assets().eventHandlers());
            }
        } else if (currentModelRenderTarget != null) {
            resetModelRenderTarget();
        }
    }

    @Nullable
    protected abstract ResourceHolder createResourceHolder(ModelRenderTargetLease lease, boolean isFallback);

    protected String requestedTextureName() {
        return "";
    }

    protected String requestedRenderTargetId() {
        return RenderTargetIds.PLAYER;
    }

    protected String fallbackRenderTargetId() {
        return requestedRenderTargetId();
    }

    protected final ResourceHolder getResourceHolder() {
        return modelBinding.resourceHolder();
    }

    protected void onModelRenderTargetLoaded(ModelRenderTarget newModel) {
        var resourceHolder = modelBinding.resourceHolder();
        if (resourceHolder == null) {
            return;
        }
        resourceHolder.soundHolder = null;
        deferHandler = newModel.assets().eventHandlers().get(MolangEventWrapper.DEFER);
    }

    protected void resetModelRenderTarget() {
        modelBinding.releaseRenderTarget();
        currentModelRenderTarget = null;
        deferHandler = null;
        modelFallback = false;
        resetGeoModel();
    }

    @Override
    protected void resetGeoModel() {
        super.resetGeoModel();
        alterPhysicsManager = null;
        lastCheckUpdateTime = 0;
    }

    public void reset() {
        modelBinding.clearModel();
        initialize = false;
        resetModelRenderTarget();
    }

    // getGeoModel 跟女仆的 IGeoEntity 冲突了，所以叫这个
    protected abstract GeoModel getYsmGeoModel();

    public final Hash256 getModelHash() {
        return modelBinding.modelHash();
    }

    /** Presentation path for GUI and third-party string APIs; never used as model identity. */
    public final String getModelId() {
        var modelHash = modelBinding.modelHash();
        return modelHash == null ? "default" : ClientModelService.instance().displayPath(modelHash);
    }

    @Override
    public boolean isModelPresent() {
        var resourceHolder = modelBinding.resourceHolder();
        return resourceHolder != null && !resourceHolder.fallback && resourceHolder.isLoaded();
    }

    @Override
    @Nullable
    public final IValue getUserFunction(String name) {
        return getModelRenderTarget().assets().userFunctions().get(name);
    }

    @Override
    public Optional<AudioStreamProvider> getSoundStream(String name) {
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
        waitForAsyncUpdate();
        UnsafeUtil.getUnsafe().storeFence();
        asyncTask = ThreadTools.submit(() -> {
            try {
                return super.update(partialTicks, RenderContext.levelImmutable());
            } finally {
                UnsafeUtil.getUnsafe().storeFence();
            }
        });
    }

    @Override
    @Nullable
    protected GeoRenderData update(float partialTicks, RenderContext context) {
        RenderSystem.assertOnRenderThread();
        var resolvedContext = resolveRenderContext(context);
        if (resolvedContext.immutable() && asyncTask != null) {
            var result = awaitAsyncUpdate();
            if (result != null) {
                return result;
            }
        }
        waitForAsyncUpdate();
        checkModelUpdate();
        return super.update(partialTicks, resolvedContext);
    }

    public void waitForAsyncUpdate() {
        awaitAsyncUpdate();
    }

    private @Nullable GeoRenderData awaitAsyncUpdate() {
        if (asyncTask != null) {
            GeoRenderData result = null;
            try {
                result = asyncTask.get();
                UnsafeUtil.getUnsafe().loadFence();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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

    protected static class ResourceHolder implements AutoCloseable {
        private final ModelRenderTargetLease lease;
        public final ModelRenderTarget model;
        public final boolean fallback;
        @Nullable
        public ModelSoundHolder soundHolder;

        protected ResourceHolder(ModelRenderTargetLease lease, boolean fallback) {
            this.lease = lease;
            this.model = lease.renderTarget();
            this.fallback = fallback;
        }

        public boolean isLoaded() {
            return true;
        }

        boolean isCurrent() {
            return lease.isCurrent();
        }

        @Override
        public void close() {
            lease.close();
        }
    }
}
