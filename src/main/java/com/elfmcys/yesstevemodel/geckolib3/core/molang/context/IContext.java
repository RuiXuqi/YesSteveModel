package com.elfmcys.yesstevemodel.geckolib3.core.molang.context;

import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IForeignVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IScopedVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.ITempVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IContext<TEntity> {
    TEntity entity();

    AnimatableEntity<?> animatableEntity();

    Minecraft mc();

    ClientLevel level();

    AnimationEvent<?> animationEvent();

    EntityModelData data();

    @Nullable
    AnimationContext animationContext();

    @Nullable
    ControllerContext controllerContext();

    RandomSource random();

    <TChild> IContext<TChild> createChild(TChild child);

    ITempVariableStorage tempStorage();

    IScopedVariableStorage scopedStorage();

    IForeignVariableStorage foreignStorage();

    @Nullable
    IValue getUserFunction(int name);

    Object callUserFunction(ExecutionContext<?> context, IValue value, List<?> args);

    Object callUserFunction(ExecutionContext<?> ctx, IValue value, Function.ArgumentCollection args);

    List<?> userFunctionArgs();

    boolean isDebugEnabled();

    /**
     * 是否允许生成行为（粒子、音效、骨骼变色、骨骼发光、相机变换等）
     */
    boolean allowEmitting();

    void debugPrint(String message, Object... args);
}
