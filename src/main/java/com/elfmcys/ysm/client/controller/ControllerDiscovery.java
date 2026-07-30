package com.elfmcys.ysm.client.controller;

import com.elfmcys.ysm.client.entity.CustomEntity;
import com.elfmcys.ysm.client.model.CommonAsset;

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
