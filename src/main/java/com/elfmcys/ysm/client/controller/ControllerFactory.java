package com.elfmcys.ysm.client.controller;

import com.elfmcys.ysm.client.entity.CustomEntity;
import com.elfmcys.ysm.geckolib3.core.controller.IAnimationController;

import java.util.function.Consumer;

public interface ControllerFactory<T extends CustomEntity<?>> {
    void create(T animatable, Consumer<IAnimationController<T>> consumer);
}
