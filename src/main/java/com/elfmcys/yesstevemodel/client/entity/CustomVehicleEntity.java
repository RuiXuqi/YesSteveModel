package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.animation.predicate.ParallelPredicate;
import com.elfmcys.yesstevemodel.client.animation.predicate.VehicleMainPredicate;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.client.model.VehicleModel;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.VEHICLE_MAIN_CONTROLLER;
import static com.elfmcys.yesstevemodel.util.ControllerUtils.VEHICLE_PARALLEL_CONTROLLER;

public class CustomVehicleEntity extends CustomEntity<Entity> {
    private VehicleModel vehicleModel;

    public CustomVehicleEntity(Entity vehicle) {
        super(vehicle, true);
        registerControllers();
    }

    @SuppressWarnings("unchecked,rawtypes,deprecation")
    private void registerControllers() {
        addAnimationController(new HybridAnimationController(this, VEHICLE_MAIN_CONTROLLER, 0.1f, new VehicleMainPredicate()));
        for (int i = 0; i < 8; i++) {
            String controllerName = VEHICLE_PARALLEL_CONTROLLER + i;
            String animationName = String.format("parallel%d", i);
            addAnimationController(new HybridAnimationController<>(this, controllerName, 0,
                    new ParallelPredicate<>(animationName), true));
        }
    }

    @Override
    protected boolean prepareForUpdate() {
        return super.prepareForUpdate() && vehicleModel != null;
    }

    /**
     * 当前载具没有模型时，由于跳过渲染而不会检查模型更新，此时依赖于异步更新的机制检查。
     */
    @Override
    @SuppressWarnings("deprecation")
    protected boolean onLoadModelContainer(ClientModel newModel, boolean isFallback) {
        vehicleModel = isFallback ? null : newModel.vehicleModels().get(entity.getType().builtInRegistryHolder().key().location());
        return vehicleModel != null;
    }

    @Override
    protected GeoModel getYsmGeoModel() {
        return vehicleModel.model();
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation() {
        return vehicleModel.texture();
    }

    @Override
    public Animation getAnimation(String name) {
        return vehicleModel.animations().get(name);
    }

    @Override
    public boolean isModelPresent() {
        return super.isModelPresent() && vehicleModel != null;
    }

    @Override
    public float getWidthScale() {
        return 0.7F;
    }

    @Override
    public float getHeightScale() {
        return 0.7F;
    }
}