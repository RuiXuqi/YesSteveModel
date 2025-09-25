package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.animation.predicate.*;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.client.model.VehicleModel;
import com.elfmcys.yesstevemodel.client.texture.CustomTextureManager;
import com.elfmcys.yesstevemodel.client.texture.TextureHolder;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.*;

public class CustomVehicleEntity extends CustomEntity<Entity> {
    private VehicleModel vehicleModel;

    public CustomVehicleEntity(Entity vehicle) {
        super(vehicle, true);
        registerControllers();
    }

    @SuppressWarnings("unchecked,rawtypes,deprecation")
    private void registerControllers() {
        for (int i = 0; i < 8; i++) {
            String controllerName = VEHICLE_PRE_PARALLEL_CONTROLLER + i;
            String animationName = String.format("pre_parallel%d", i);
            addAnimationController(new HybridAnimationController<>(this, controllerName, 0,
                    new ParallelPredicate<>(animationName), true));
        }

        addAnimationController(new HybridAnimationController(this, VEHICLE_PRE_MAIN_CONTROLLER, 0, new EmptyPredicate()));
        addAnimationController(new HybridAnimationController(this, VEHICLE_MAIN_CONTROLLER, 0.1f, new VehicleMainPredicate()));
        addAnimationController(new HybridAnimationController(this, VEHICLE_MOVE_CONTROLLER, 0.1f, new VehicleMovePredicate()));
        addAnimationController(new HybridAnimationController(this, VEHICLE_RIDE_CONTROLLER, 0.1f, new VehicleRidePredicate()));
        addAnimationController(new HybridAnimationController(this, VEHICLE_POST_MAIN_CONTROLLER, 0, new EmptyPredicate()));

        for (int i = 0; i < 8; i++) {
            String controllerName = VEHICLE_PARALLEL_CONTROLLER + i;
            String animationName = String.format("parallel%d", i);
            addAnimationController(new HybridAnimationController<>(this, controllerName, 0,
                    new ParallelPredicate<>(animationName), true));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected @Nullable ResourceHolder createResourceHolder(ClientModel model) {
        var vehicleModel = model.vehicleModels().get(entity.getType().builtInRegistryHolder().key().location());
        if (vehicleModel != null) {
            return new VehicleResourceHolder(model, vehicleModel);
        }
        return null;
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
        return ((VehicleResourceHolder) getResourceHolder()).textureHolder.id().orElseGet(MissingTextureAtlasSprite::getLocation);
    }

    @Override
    public Animation getAnimation(String name) {
        return vehicleModel.animations().get(name);
    }

    @Override
    public @Nullable AnimationControllerData getAnimationControllerData(String animationControllerName) {
        return vehicleModel.controllers().get(animationControllerName);
    }

    @Override
    public boolean isModelPresent() {
        return super.isModelPresent() && vehicleModel != null && getResourceHolder().isLoaded();
    }

    @Override
    public float getWidthScale() {
        return 0.7F;
    }

    @Override
    public float getHeightScale() {
        return 0.7F;
    }

    private static class VehicleResourceHolder extends ResourceHolder {
        private final TextureHolder textureHolder;

        protected VehicleResourceHolder(ClientModel model, VehicleModel vehicleModel) {
            super(model);
            textureHolder = CustomTextureManager.register(vehicleModel.texture(), true);
        }

        @Override
        public boolean isLoaded() {
            return textureHolder.id().isPresent();
        }
    }
}