package com.elfmcys.yesstevemodel.client.controller.collections;

import com.elfmcys.yesstevemodel.client.animation.condition.ConditionArmor;
import com.elfmcys.yesstevemodel.client.animation.predicate.*;
import com.elfmcys.yesstevemodel.client.controller.*;
import com.elfmcys.yesstevemodel.client.entity.CustomProjectileEntity;
import com.elfmcys.yesstevemodel.client.model.CommonAsset;
import com.elfmcys.yesstevemodel.client.model.ProjectileModel;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ProjectileControllerCollection {
    private static final String CATEGORY = "projectile";
    private static final AnimationControllerCollection<CustomProjectileEntity, ProjectileModel> COLLECTION = new AnimationControllerCollection<>();

    @SuppressWarnings("rawtypes,unchecked,deprecation")
    private static void init() {
        single("pre_main", null, true, (name, entity) -> new HybridAnimationController(entity, name, 0, new EmptyPredicate()));
        single("main", ProjectileMainPredicate.ANIM_LIST, true, (name, entity) -> new HybridAnimationController(entity, name, 0.1f, new ProjectileMainPredicate()));
        single("post_main", null, true, (name, entity) -> new HybridAnimationController(entity, name, 0, new EmptyPredicate()));

        parallel("parallel", (name, entity, anim) ->
                new HybridAnimationController(entity, name, 0, anim != null ? new ParallelPredicate(anim) : EmptyPredicate.INSTANCE, true));
    }

    public static Consumer<CustomProjectileEntity> build(ProjectileModel model, CommonAsset assets) {
        if (COLLECTION.isEmpty()) {
            init();
        }
        return COLLECTION.build(model, assets);
    }

    private static ControllerDiscovery<CustomProjectileEntity, ProjectileModel> simple(String name, BiFunction<String, CustomProjectileEntity, IAnimationController<CustomProjectileEntity>> simpleFactory) {
        var controllerName = String.format("%s.%s", CATEGORY, name);
        return COLLECTION.add((model, container) -> (animatable, consumer) -> {
            consumer.accept(simpleFactory.apply(controllerName, animatable));
        });
    }

    private static ControllerDiscovery<CustomProjectileEntity, ProjectileModel> single(String name, String[] animations, boolean hybrid, BiFunction<String, CustomProjectileEntity, IAnimationController<CustomProjectileEntity>> controllerFunc) {
        return COLLECTION.add(new SingleControllerDiscovery<>(CATEGORY, name, animations, hybrid, Adapter.INSTANCE, controllerFunc));
    }

    private static ControllerDiscovery<CustomProjectileEntity, ProjectileModel> parallel(String name, TriFunction<String, CustomProjectileEntity, String, IAnimationController<CustomProjectileEntity>> controllerFunc) {
        return COLLECTION.add(new ParallelControllerDiscovery<>(CATEGORY, name, false, Adapter.INSTANCE, controllerFunc));
    }

    private static class Adapter implements ResourceAdapter<ProjectileModel> {
        static final Adapter INSTANCE = new Adapter();

        @Override
        public Object2ReferenceMap<String, AnimationControllerData> getControllers(ProjectileModel model, CommonAsset assets) {
            return model.controllers();
        }

        @Override
        public Object2ReferenceMap<String, Animation> getAnimations(ProjectileModel model, CommonAsset assets) {
            return model.animations();
        }

        @Override
        public ConditionArmor getArmorCondition(ProjectileModel model, CommonAsset assets) {
            return null;
        }
    }
}
