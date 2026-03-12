package com.elfmcys.yesstevemodel.client.controller;

import com.elfmcys.yesstevemodel.client.entity.CustomEntity;
import com.elfmcys.yesstevemodel.client.model.CommonAsset;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ParallelControllerDiscovery<T extends CustomEntity<?>, TModel> implements ControllerDiscovery<T, TModel> {
    private final String category;
    private final String name;
    private final Predicate<String> controllerNamePattern;
    private final Predicate<String> eventNamePattern;
    private final Predicate<String> animationNamePattern;
    private final ResourceAdapter<TModel> resourceAdapter;
    private final TriFunction<String, T, String, IAnimationController<T>> controllerFunc;

    public ParallelControllerDiscovery(String category, String name, boolean multi, ResourceAdapter<TModel> resourceAdapter, TriFunction<String, T, String, IAnimationController<T>> controllerFunc) {
        this.category = category;
        this.name = name;
        if (multi) {
            this.controllerNamePattern = Pattern.compile(String.format("^%s\\.%s_.+", category, name)).asMatchPredicate();
            this.eventNamePattern = Pattern.compile(String.format("^%s_ctrl_%s_.+", category, name)).asMatchPredicate();
        } else {
            this.controllerNamePattern = Pattern.compile(String.format("^%s\\.%s_[0-7]$", category, name)).asMatchPredicate();
            this.eventNamePattern = Pattern.compile(String.format("^%s_ctrl_%s_[0-7]$", category, name)).asMatchPredicate();
        }
        this.animationNamePattern = Pattern.compile(String.format("^%s[0-7]$", name)).asMatchPredicate();
        this.resourceAdapter = resourceAdapter;
        this.controllerFunc = controllerFunc;
    }

    @Override
    public ControllerFactory<T> process(TModel model, CommonAsset assets) {
        var names = new ObjectRBTreeSet<Pair<String, String>>();
        Object2ReferenceMaps.fastForEach(resourceAdapter.getControllers(model, assets), entry -> {
            if (controllerNamePattern.test(entry.getKey())) {
                names.add(Pair.of(entry.getKey(), null));
            }
        });
        Object2ReferenceMaps.fastForEach(assets.eventHandlers(), entry -> {
            if (eventNamePattern.test(entry.getKey())) {
                var controllerName = entry.getKey().replace("_ctrl_", ".");
                names.add(Pair.of(controllerName, null));
            }
        });
        Object2ReferenceMaps.fastForEach(resourceAdapter.getAnimations(model, assets), entry -> {
            if (!entry.getValue().isEmpty() && animationNamePattern.test(entry.getKey())) {
                var controllerName = String.format("%s.%s_%s", category, name, entry.getKey().substring(name.length()));
                names.add(Pair.of(controllerName, entry.getKey()));
            }
        });
        return ((animatable, consumer) -> {
            for (var name : names) {
                consumer.accept(controllerFunc.apply(name.getLeft(), animatable, name.getRight()));
            }
        });
    }
}
