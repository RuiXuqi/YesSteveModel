package com.elfmcys.ysm.client.entity;

import com.elfmcys.ysm.client.controller.VehicleOriginController;
import com.elfmcys.ysm.client.controller.collections.VehicleControllerCollection;
import com.elfmcys.ysm.client.model.ClientModel;
import com.elfmcys.ysm.client.model.VehicleModel;
import com.elfmcys.ysm.client.texture.CustomTextureManager;
import com.elfmcys.ysm.client.texture.TextureHolder;
import com.elfmcys.ysm.geckolib3.core.builder.Animation;
import com.elfmcys.ysm.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class CustomVehicleEntity extends CustomEntity<Entity> {
    private VehicleModel vehicleModel;
    private VehicleOriginController originController;

    public CustomVehicleEntity(Entity vehicle) {
        super(vehicle, true);
    }

    @Override
    protected void onSetupAnimationController() {
        if (vehicleModel != null) {
            vehicleModel.controllerFactory().accept(this);
            originController = (VehicleOriginController) getAnimationData().getAnimationController(VehicleControllerCollection.NAME_ORIGIN);
        }
    }

    @Nullable
    public Vector3f getRotation() {
        if (originController != null) {
            return originController.getRotation();
        }
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected @Nullable ResourceHolder createResourceHolder(ClientModel model, boolean isFallback) {
        var vehicleModel = model.vehicleModels().get(entity.getType().builtInRegistryHolder().key().location());
        if (vehicleModel != null) {
            return new VehicleResourceHolder(model, isFallback, vehicleModel);
        }
        return null;
    }

    /**
     * 当前载具没有模型时，由于跳过渲染而不会检查模型更新，此时依赖于异步更新的机制检查。
     */
    @Override
    @SuppressWarnings("deprecation")
    protected void onLoadModelContainer(ClientModel newModel) {
        super.onLoadModelContainer(newModel);
        vehicleModel = newModel.vehicleModels().get(entity.getType().builtInRegistryHolder().key().location());
    }

    @Override
    public void resetModelContainer() {
        super.resetModelContainer();
        this.vehicleModel = null;
        this.originController = null;
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

        protected VehicleResourceHolder(ClientModel model, boolean fallback, VehicleModel vehicleModel) {
            super(model, fallback);
            textureHolder = CustomTextureManager.register(vehicleModel.texture(), true);
        }

        @Override
        public boolean isLoaded() {
            return textureHolder.id().isPresent();
        }
    }
}