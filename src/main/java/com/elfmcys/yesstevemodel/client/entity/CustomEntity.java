package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.AnimationParallelTicker;
import com.elfmcys.yesstevemodel.client.animation.debug.CustomDebugSource;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.client.sound.SoundData;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * 自动管理当前 model id 和 model container，并在找不到指定模型时 fallback 到默认模型
 */
public abstract class CustomEntity<T extends Entity> extends AnimatableEntity<T> {
    private String modelId = ModelIdUtil.DEFAULT_MODEL_ID;
    private ClientModel currentModelContainer;
    private ResourceHolder resourceHolder;
    private boolean modelFallback;
    private int lastCheckUpdateTime;

    protected CustomEntity(T entity, boolean asyncUpdate) {
        super(entity);
        if (asyncUpdate) {
            AnimationParallelTicker.register(this);
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
        waitForAsyncUpdate();
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

        if (resourceHolder != null && (resourceHolder.model != currentModelContainer || resourceHolder.fallback != modelFallback) && resourceHolder.isLoaded()) {
            currentModelContainer = resourceHolder.model;
            modelFallback = resourceHolder.fallback;
            onLoadModelContainer(currentModelContainer);
            loadGeoModel(getYsmGeoModel(), currentModelContainer.assets().eventHandlers());
        }
    }

    @Nullable
    protected abstract ResourceHolder createResourceHolder(ClientModel model, boolean isFallback);

    protected final ResourceHolder getResourceHolder() {
        return resourceHolder;
    }

    protected void onLoadModelContainer(ClientModel newModel) {
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
    @Nullable
    public final IValue getUserFunction(int name) {
        return getModelContainer().assets().userFunctions().get(name);
    }

    @Override
    @Nullable
    public final SoundData getSoundData(String name) {
        return getModelContainer().assets().sounds().get(name);
    }

    @Override
    public DebugSource getDebugSource() {
        if (DebugAnimationKey.TYPE != DebugAnimationKey.DebugType.NONE) {
            return CustomDebugSource.INSTANCE;
        } else {
            return null;
        }
    }

    protected static class ResourceHolder {
        public final ClientModel model;
        public final boolean fallback;

        protected ResourceHolder(ClientModel model, boolean fallback) {
            this.model = model;
            this.fallback = fallback;
        }

        public boolean isLoaded() {
            return true;
        }
    }
}
