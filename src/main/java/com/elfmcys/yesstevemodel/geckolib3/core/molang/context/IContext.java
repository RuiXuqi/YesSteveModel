package com.elfmcys.yesstevemodel.geckolib3.core.molang.context;

import com.elfmcys.yesstevemodel.geckolib3.core.controller.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IForeignVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IScopedVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.ITempVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import javax.annotation.Nullable;
import java.util.Random;

public interface IContext<TEntity> {
    TEntity entity();

    AnimatableEntity<?> animatableEntity();

    Minecraft mc();

    ClientLevel level();

    AnimationEvent<?> animationEvent();

    EntityModelData data();

    // FIXME: 莫名其妙会为 null
    @Nullable
    AnimationContext animationContext();

    Random random();

    <TChild> IContext<TChild> createChild(TChild child);

    ITempVariableStorage tempStorage();

    IScopedVariableStorage scopedStorage();

    IForeignVariableStorage foreignStorage();

    boolean isDebugEnabled();

    void debugPrint(String message, Object... args);
}
