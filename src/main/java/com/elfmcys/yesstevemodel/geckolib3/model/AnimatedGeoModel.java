package com.elfmcys.yesstevemodel.geckolib3.model;

import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatableModel;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.manager.AnimationData;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IForeignVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.AnimationProcessor;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.DebugInfo;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.GeoModelProvider;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.Blaze3D;
import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public abstract class AnimatedGeoModel<T extends IAnimatable<?>> extends GeoModelProvider<T> implements IAnimatableModel<T> {
    private final AnimationProcessor<T> animationProcessor;
    private GeoModelState currentModel;

    protected AnimatedGeoModel() {
        this.animationProcessor = new AnimationProcessor<>(this);
    }

    @Override
    public boolean setCustomAnimations(T animatable, AnimationContext<?> ctx, @NotNull AnimationEvent<T> animationEvent) {
        Minecraft mc = Minecraft.getInstance();
        AnimationData manager = animatable.getFactory().getOrCreateAnimationData(0, this);
        AnimationEvent<T> predicate;

        boolean forceUpdate = this.forceUpdate();
        double currentTick = forceUpdate ? (Blaze3D.getTime() * 20) : getCurrentTick();

        if (manager.startTick == -1) {
            manager.startTick = currentTick;
        } else {
            manager.tick = currentTick - manager.startTick;
            if (!mc.isPaused() || manager.shouldPlayWhilePaused) {
                double deltaTicks = manager.tick - this.lastGameTickTime;
                this.seekTime += deltaTicks;
            }
            this.lastGameTickTime = manager.tick;
        }

        predicate = animationEvent;
        predicate.animationTick = this.seekTime;
        getAnimationProcessor().putRemoteStruct(getRemoteStruct(animatable));
        getAnimationProcessor().preAnimationSetup(predicate.getAnimatable(), this.seekTime);
        if (!getAnimationProcessor().isModelRendererEmpty()) {
            return getAnimationProcessor().tickAnimation(animatable, this.seekTime, forceUpdate, predicate, ctx, this.shouldCrashOnMissing);
        }
        return false;
    }

    @Override
    public AnimationProcessor<T> getAnimationProcessor() {
        return this.animationProcessor;
    }

    public boolean updateCurrentModel(T animatable) {
        String mainModelId = getModelLocation(animatable);
        GeoModel model = getModel(mainModelId);
        if (model == null) {
            this.currentModel = null;
            return false;
        }
        if (this.currentModel == null || model != this.currentModel.model()) {
            this.currentModel = new GeoModelState(model);
            this.animationProcessor.registerModelRenderer(currentModel.boneMap());
        }
        return true;
    }

    public GeoModelState getCurrentModel() {
        return currentModel;
    }

    @Override
    public double getCurrentTick() {
        return RenderUtils.getRenderTickTime();
    }

    public boolean forceUpdate() {
        return false;
    }

    public DebugInfo getDebugInfo() {
        return animationProcessor.getDebugInfo();
    }

    public void execute(IValue value, @Nullable Consumer<Object> resultConsumer) {
        animationProcessor.execute(value, resultConsumer);
    }

    public IForeignVariableStorage getPublicVariableStorage() {
        return this.animationProcessor.getPublicVariableStorage();
    }
}
