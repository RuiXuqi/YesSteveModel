package com.elfmcys.yesstevemodel.geckolib3.core.molang.context;

import com.elfmcys.yesstevemodel.geckolib3.core.controller.AnimationControllerContext;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IForeignVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IScopedVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.ITempVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoInstance;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Random;

public interface IContext<TEntity> {
    TEntity entity();

    GeoInstance<?, ?> geoInstance();

    Minecraft mc();

    ClientLevel level();

    AnimationEvent<?> animationEvent();

    EntityModelData data();

    AnimationControllerContext animationControllerContext();

    Random random();

    <TChild> IContext<TChild> createChild(TChild child);

    ITempVariableStorage tempStorage();

    IScopedVariableStorage scopedStorage();

    IForeignVariableStorage foreignStorage();

    boolean isDebugEnabled();

    void debugPrint(String message, Object... args);
}
