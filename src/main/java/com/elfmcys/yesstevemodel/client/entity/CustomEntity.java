package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
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
    private boolean isFallback;

    protected CustomEntity(T entity, boolean asyncUpdate) {
        super(entity, asyncUpdate);
    }

    @Override
    protected boolean prepareForUpdate() {
        checkModelContainerUpdate();
        return true;
    }

    public final ClientModel getModelContainer() {
        return currentModelContainer;
    }

    protected final void updateModelId(String modelId) {
        this.modelId = modelId;
        checkModelContainerUpdate();
    }

    private void checkModelContainerUpdate() {
        var updated = ClientModelManager.getModel(modelId).map(model -> {
            if (isFallback || model != currentModelContainer) {
                isFallback = false;
                currentModelContainer = model;
                return true;
            }
            return false;
        }).orElseGet(() -> {
            var defaultModel = ClientModelManager.getDefaultModel();
            if (!isFallback || defaultModel != currentModelContainer) {
                isFallback = true;
                currentModelContainer = defaultModel;
                return true;
            }
            return false;
        });

        if (updated && onLoadModelContainer(currentModelContainer, isFallback)) {
            loadGeoModel(getYsmGeoModel(), currentModelContainer.assets().eventHandlers());
        }
    }

    protected boolean onLoadModelContainer(ClientModel newModel, boolean isFallback) {
        return true;
    }

    // getGeoModel 跟女仆的 IGeoEntity 冲突了，所以叫这个
    protected abstract GeoModel getYsmGeoModel();

    public final String getModelId() {
        return modelId;
    }

    @Override
    public boolean isModelPresent() {
        return !isFallback;
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
}
