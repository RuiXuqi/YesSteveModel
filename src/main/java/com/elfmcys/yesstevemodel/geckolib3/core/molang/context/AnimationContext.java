package com.elfmcys.yesstevemodel.geckolib3.core.molang.context;

import com.elfmcys.yesstevemodel.capability.ArrowGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.AnimationControllerContext;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IForeignVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IScopedVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.ITempVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.VariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;

import java.util.Random;

public class AnimationContext<TEntity> implements IContext<TEntity> {
    protected final TEntity entity;
    protected final AnimatableEntity<?> animatableEntity;
    protected final AnimationEvent<?> animationEvent;
    protected final EntityModelData data;

    protected AnimationControllerContext animationControllerContext;
    protected Random random;
    protected VariableStorage storage;
    protected IForeignVariableStorage foreignStorage;
    private DebugSource debugSource;

    public AnimationContext(TEntity entity, AnimatableEntity<?> animatableEntity, AnimationEvent<?> animationEvent, EntityModelData data) {
        this.entity = entity;
        this.animatableEntity = animatableEntity;
        this.animationEvent = animationEvent;
        this.data = data;
    }

    private AnimationContext(TEntity entity, AnimatableEntity<?> animatableEntity, AnimationEvent<?> animationEvent, EntityModelData data, AnimationControllerContext animationControllerContext, Random random, VariableStorage storage) {
        this.entity = entity;
        this.animatableEntity = animatableEntity;
        this.animationEvent = animationEvent;
        this.data = data;
        this.animationControllerContext = animationControllerContext;
        this.random = random;
        this.storage = storage;
        if (entity instanceof Player) {
            ((Entity) entity).getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
                foreignStorage = cap.getPublicVariableStorage();
            });
        } else if (entity instanceof AbstractArrow) {
            ((AbstractArrow) entity).getCapability(ArrowGeoCapabilityProvider.CAP).ifPresent(cap -> {
                foreignStorage = cap.getPublicVariableStorage();
            });
        }
    }

    @Override
    public AnimationEvent<?> animationEvent() {
        return animationEvent;
    }

    @Override
    public AnimatableEntity<?> animatableEntity() {
        return animatableEntity;
    }

    @Override
    public EntityModelData data() {
        return data;
    }

    @Override
    public AnimationControllerContext animationControllerContext() {
        return animationControllerContext;
    }

    @Override
    public Random random() {
        return random;
    }

    @Override
    public TEntity entity() {
        return entity;
    }

    @Override
    public Minecraft mc() {
        return Minecraft.getInstance();
    }

    @Override
    public ClientLevel level() {
        Minecraft mc = mc();
        if (mc != null) {
            return mc.level;
        } else {
            return null;
        }
    }

    @Override
    public <TChild> IContext<TChild> createChild(TChild child) {
        return new AnimationContext<>(child, animatableEntity, animationEvent, data, animationControllerContext, random, storage);
    }

    @Override
    public ITempVariableStorage tempStorage() {
        return storage;
    }

    @Override
    public IScopedVariableStorage scopedStorage() {
        return storage;
    }

    @Override
    public IForeignVariableStorage foreignStorage() {
        return foreignStorage;
    }

    @Override
    public boolean isDebugEnabled() {
        return debugSource != null;
    }

    @Override
    public void debugPrint(String message, Object...args) {
        if(isDebugEnabled()) {
            debugSource.print(message, args);
        }
    }

    public void setAnimationControllerContext(AnimationControllerContext animationControllerContext) {
        this.animationControllerContext = animationControllerContext;
    }

    public void setStorage(VariableStorage storage) {
        this.storage = storage;
        this.foreignStorage = storage;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public void setDebugSource(DebugSource source) {
        this.debugSource = source;
    }
}
