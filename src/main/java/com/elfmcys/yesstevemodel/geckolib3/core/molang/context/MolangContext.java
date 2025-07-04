package com.elfmcys.yesstevemodel.geckolib3.core.molang.context;

import com.elfmcys.yesstevemodel.capability.ArrowGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IForeignVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IScopedVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.ITempVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.MolangMemory;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import com.elfmcys.yesstevemodel.molang.runtime.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MolangContext<TEntity> implements IContext<TEntity> {
    protected final TEntity entity;
    protected final AnimatableEntity<?> animatableEntity;
    protected final AnimationEvent<?> animationEvent;
    protected final EntityModelData data;

    protected AnimationContext animationContext;
    protected ControllerContext controllerContext;
    protected RandomSource random;
    protected MolangMemory memory;
    protected IForeignVariableStorage foreignStorage;
    private DebugSource debugSource;
    private boolean allowEmitting;

    public MolangContext(TEntity entity, AnimatableEntity<?> animatableEntity, AnimationEvent<?> animationEvent, EntityModelData data) {
        this.entity = entity;
        this.animatableEntity = animatableEntity;
        this.animationEvent = animationEvent;
        this.data = data;
    }

    private MolangContext(TEntity entity, AnimatableEntity<?> animatableEntity, AnimationEvent<?> animationEvent, EntityModelData data, AnimationContext animationContext, RandomSource random, MolangMemory memory) {
        this.entity = entity;
        this.animatableEntity = animatableEntity;
        this.animationEvent = animationEvent;
        this.data = data;
        this.animationContext = animationContext;
        this.random = random;
        this.memory = memory;
        if (entity instanceof Player) {
            ((Entity) entity).getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
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
    public AnimationContext animationContext() {
        return animationContext;
    }

    @Override
    public ControllerContext controllerContext() {
        return controllerContext;
    }

    @Override
    public RandomSource random() {
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

    // FIXME: 需要同时更新 animatable 和 entity 两个属性，再加上源属性
    @Override
    public <TChild> IContext<TChild> createChild(TChild child) {
        return new MolangContext<>(child, animatableEntity, animationEvent, data, animationContext, random, memory);
    }

    @Override
    public ITempVariableStorage tempStorage() {
        return memory.getStackMemory();
    }

    @Override
    public IScopedVariableStorage scopedStorage() {
        return memory;
    }

    @Override
    public IForeignVariableStorage foreignStorage() {
        return foreignStorage;
    }

    @Override
    public @Nullable IValue getUserFunction(int name) {
        return animatableEntity.getUserFunction(name);
    }

    @Override
    public Object callUserFunction(ExecutionContext<?> ctx, IValue value, List<?> args) {
        if (this.memory.getStackMemory().push(args)) {
            try {
                return value.eval((ExpressionEvaluator<?>) ctx);
            } finally {
                this.memory.getStackMemory().pop();
            }
        }
        return null;
    }

    @Override
    public Object callUserFunction(ExecutionContext<?> ctx, IValue value, Function.ArgumentCollection args) {
        if (this.memory.getStackMemory().push(ctx, args)) {
            try {
                return value.eval((ExpressionEvaluator<?>) ctx);
            } finally {
                this.memory.getStackMemory().pop();
            }
        }
        return null;
    }

    @Override
    public List<?> userFunctionArgs() {
        return memory.getStackMemory().argsAccessor();
    }

    @Override
    public boolean isDebugEnabled() {
        return debugSource != null;
    }

    @Override
    public boolean allowEmitting() {
        return allowEmitting;
    }

    public void setAllowEmitting(boolean value) {
        allowEmitting = value;
    }

    @Override
    public void debugPrint(String message, Object...args) {
        if(isDebugEnabled()) {
            debugSource.print(message, args);
        }
    }

    public void setAnimationContext(AnimationContext ctx) {
        this.animationContext = ctx;
    }

    public void setControllerContext(ControllerContext ctx) {
        this.controllerContext = ctx;
    }

    public void setMemory(MolangMemory storage) {
        this.memory = storage;
        this.foreignStorage = storage;
    }

    public void setRandom(RandomSource random) {
        this.random = random;
    }

    public void setDebugSource(DebugSource source) {
        this.debugSource = source;
    }
}
