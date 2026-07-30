package com.elfmcys.ysm.client.entity;

import com.elfmcys.ysm.client.controller.VehicleOriginController;
import com.elfmcys.ysm.client.controller.collections.VehicleControllerCollection;
import com.elfmcys.ysm.client.model.ModelRenderTarget;
import com.elfmcys.ysm.client.model.VehicleModelResources;
import com.elfmcys.ysm.client.model.ClientModelService;
import com.elfmcys.ysm.client.model.ModelRenderTargetLease;
import com.elfmcys.ysm.client.texture.CustomTextureManager;
import com.elfmcys.ysm.client.texture.TextureHolder;
import com.elfmcys.ysm.geckolib3.core.builder.Animation;
import com.elfmcys.ysm.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoModel;
import mixel.manifest.asset.RenderTargetOuterClass;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class CustomVehicleEntity extends CustomEntity<Entity> {
    private VehicleModelResources vehicleResources;
    private VehicleOriginController originController;

    public CustomVehicleEntity(Entity vehicle) {
        super(vehicle, true);
    }

    @Override
    protected String requestedRenderTargetId() {
        var hash = getModelHash();
        return hash == null ? null : ClientModelService.instance()
                .findRenderTarget(hash,
                        RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_VEHICLE,
                        entity.getType().builtInRegistryHolder().key().location()).orElse(null);
    }

    @Override
    protected String fallbackRenderTargetId() {
        return ClientModelService.instance()
                .findDefaultRenderTarget(
                        RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_VEHICLE,
                        entity.getType().builtInRegistryHolder().key().location()).orElse(null);
    }

    @Override
    protected void onSetupAnimationController() {
        if (vehicleResources != null) {
            vehicleResources.controllerFactory().accept(this);
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
    protected @Nullable ResourceHolder createResourceHolder(ModelRenderTargetLease lease, boolean isFallback) {
        var model = lease.renderTarget();
        var vehicleResources = model.vehicleResources();
        if (vehicleResources != null) {
            return new VehicleResourceHolder(lease, isFallback, vehicleResources);
        }
        return null;
    }

    /**
     * 当前载具没有模型时，由于跳过渲染而不会检查模型更新，此时依赖于异步更新的机制检查。
     */
    @Override
    @SuppressWarnings("deprecation")
    protected void onModelRenderTargetLoaded(ModelRenderTarget newModel) {
        super.onModelRenderTargetLoaded(newModel);
        vehicleResources = newModel.vehicleResources();
    }

    @Override
    public void resetModelRenderTarget() {
        super.resetModelRenderTarget();
        this.vehicleResources = null;
        this.originController = null;
    }

    @Override
    protected GeoModel getYsmGeoModel() {
        return vehicleResources.model();
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation() {
        return ((VehicleResourceHolder) getResourceHolder()).textureHolder.id().orElseGet(MissingTextureAtlasSprite::getLocation);
    }

    @Override
    public Animation getAnimation(String name) {
        return vehicleResources.animations().get(name);
    }

    @Override
    public @Nullable AnimationControllerData getAnimationControllerData(String animationControllerName) {
        return vehicleResources.controllers().get(animationControllerName);
    }

    @Override
    public boolean isModelPresent() {
        return super.isModelPresent() && vehicleResources != null && getResourceHolder().isLoaded();
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

        protected VehicleResourceHolder(ModelRenderTargetLease lease, boolean fallback, VehicleModelResources vehicleResources) {
            super(lease, fallback);
            textureHolder = CustomTextureManager.register(vehicleResources.texture(), true);
        }

        @Override
        public boolean isLoaded() {
            return textureHolder.id().isPresent();
        }
    }
}
