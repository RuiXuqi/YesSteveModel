package com.elfmcys.yesstevemodel.geckolib3.model;

import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatableModel;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.manager.AnimationData;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IForeignVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.AnimationProcessor;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.DebugInfo;
import com.elfmcys.yesstevemodel.geckolib3.file.AnimationFile;
import com.elfmcys.yesstevemodel.geckolib3.geo.exception.GeckoLibException;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.GeoModelProvider;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.IAnimatableModelProvider;
import com.elfmcys.yesstevemodel.geckolib3.resource.GeckoLibCache;
import com.elfmcys.yesstevemodel.mixin.client.MinecraftAccessor;
import com.elfmcys.yesstevemodel.mixin.client.TimerAccessor;
import com.mojang.blaze3d.Blaze3D;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public abstract class AnimatedGeoModel<T extends IAnimatable<?>> extends GeoModelProvider<T> implements IAnimatableModel<T>, IAnimatableModelProvider<T> {
    private final AnimationProcessor<T> animationProcessor;
    private GeoModelState currentModel;

    protected AnimatedGeoModel() {
        this.animationProcessor = new AnimationProcessor<>(this);
    }

    @Override
    public boolean setCustomAnimations(T animatable, AnimationContext<?> ctx, @Nonnull AnimationEvent<T> animationEvent) {
        Minecraft mc = Minecraft.getInstance();
        AnimationData manager = animatable.getFactory().getOrCreateAnimationData(0, this);
        AnimationEvent<T> predicate;
        double currentTick = getCurrentTick();

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
            return getAnimationProcessor().tickAnimation(animatable, this.seekTime, predicate, ctx, this.shouldCrashOnMissing);
        }
        return false;
    }

    @Override
    public AnimationProcessor<T> getAnimationProcessor() {
        return this.animationProcessor;
    }

    @Override
    public Animation getAnimation(String name, T animatable) {
        AnimationFile animation = GeckoLibCache.getInstance().getAnimations().get(this.getAnimationFileLocation(animatable));
        if (animation == null) {
            throw new GeckoLibException(this.getAnimationFileLocation(animatable), "Could not find animation file. Please double check name.");
        }
        return animation.getAnimation(name);
    }

    public boolean updateCurrentModel(T animatable) {
        ResourceLocation mainModelId = getModelLocation(animatable);
        GeoModel model = super.getModel(mainModelId);
        if (model == null) {
            this.currentModel = null;
            return false;
        }
        if (this.currentModel == null || model != this.currentModel.model()) {
            this.currentModel = new GeoModelState(model);
            this.animationProcessor.registerModelRenderer(currentModel.boneMap(), model.properties.scripts());
        }
        return true;
    }

    public GeoModelState getCurrentModel() {
        return currentModel;
    }

    @Override
    public double getCurrentTick() {
        if (forceUpdate()) {
            return Blaze3D.getTime() * 20;
        } else {
            return ((TimerAccessor) ((MinecraftAccessor) Minecraft.getInstance()).getTimer()).getLastMs() / 50d;
        }
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
