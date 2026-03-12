package com.elfmcys.yesstevemodel.client.controller;

import com.elfmcys.yesstevemodel.client.entity.CustomEntity;
import com.elfmcys.yesstevemodel.client.model.CommonAsset;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.util.function.Consumer;

public class AnimationControllerCollection<T extends CustomEntity<?>, TModel> {
    private final ReferenceArrayList<ControllerDiscovery<T, TModel>> discoveries = new ReferenceArrayList<>();

    public boolean isEmpty() {
        return discoveries.isEmpty();
    }

    public Consumer<T> build(TModel model, CommonAsset assets) {
        var factories = new ReferenceArrayList<ControllerFactory<T>>(discoveries.size());
        for (var discovery : discoveries) {
            factories.add(discovery.process(model, assets));
        }
        return animatable -> {
            Consumer<IAnimationController<T>> consumer = animatable::addAnimationController;
            for (var factory : factories) {
                factory.create(animatable, consumer);
            }
        };
    }

    public ControllerDiscovery<T, TModel> add(ControllerDiscovery<T, TModel> factory) {
        discoveries.add(factory);
        return factory;
    }
}
