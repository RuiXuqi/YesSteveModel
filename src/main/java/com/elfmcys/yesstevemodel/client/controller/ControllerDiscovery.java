package com.elfmcys.yesstevemodel.client.controller;

import com.elfmcys.yesstevemodel.client.entity.CustomEntity;
import com.elfmcys.yesstevemodel.client.model.CommonAsset;

import java.util.function.Predicate;

public interface ControllerDiscovery<T extends CustomEntity<?>, TModel> {
    ControllerFactory<T> process(TModel model, CommonAsset assets);

    default ControllerDiscovery<T, TModel> withCondition(Predicate<T> test) {
        return (model, asset) -> {
            var collection = process(model, asset);
            return (animatable, consumer) -> {
                if (test.test(animatable)) {
                    collection.create(animatable, consumer);
                }
            };
        };
    }
}
