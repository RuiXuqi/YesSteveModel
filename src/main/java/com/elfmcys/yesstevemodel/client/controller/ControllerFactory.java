package com.elfmcys.yesstevemodel.client.controller;

import com.elfmcys.yesstevemodel.client.entity.CustomEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;

import java.util.function.Consumer;

public interface ControllerFactory<T extends CustomEntity<?>> {
    void create(T animatable, Consumer<IAnimationController<T>> consumer);
}
