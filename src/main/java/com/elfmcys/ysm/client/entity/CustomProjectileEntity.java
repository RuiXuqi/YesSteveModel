package com.elfmcys.ysm.client.entity;

import com.elfmcys.ysm.client.model.ModelRenderTarget;
import com.elfmcys.ysm.client.model.ProjectileModelResources;
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
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomProjectileEntity extends CustomEntity<Projectile> {
    private ProjectileModelResources projectileResources;

    public CustomProjectileEntity(Projectile projectile) {
        super(projectile, true);
    }

    @Override
    protected String requestedRenderTargetId() {
        var hash = getModelHash();
        return hash == null ? null : ClientModelService.instance()
                .findRenderTarget(hash,
                        RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_PROJECTILE,
                        entity.getType().builtInRegistryHolder().key().location()).orElse(null);
    }

    @Override
    protected String fallbackRenderTargetId() {
        return ClientModelService.instance()
                .findDefaultRenderTarget(
                        RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_PROJECTILE,
                        entity.getType().builtInRegistryHolder().key().location()).orElse(null);
    }

    @Override
    protected void onSetupAnimationController() {
        if (projectileResources != null) {
            projectileResources.controllerFactory().accept(this);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected @Nullable ResourceHolder createResourceHolder(ModelRenderTargetLease lease, boolean isFallback) {
        var model = lease.renderTarget();
        if (!isFallback) {
            var projectileResources = model.projectileResources();
            if (projectileResources != null) {
                return new ProjectileResourceHolder(lease, false, projectileResources);
            }
        }
        return null;
    }

    /**
     * 当前箭矢没有模型时，由于跳过渲染而不会检查模型更新，此时依赖于异步更新的机制检查。
     */
    @Override
    @SuppressWarnings("deprecation")
    protected void onModelRenderTargetLoaded(ModelRenderTarget newModel) {
        super.onModelRenderTargetLoaded(newModel);
        projectileResources = newModel.projectileResources();
    }

    @Override
    public void resetModelRenderTarget() {
        super.resetModelRenderTarget();
        projectileResources = null;
    }

    @Override
    protected GeoModel getYsmGeoModel() {
        return projectileResources.model();
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation() {
        return ((ProjectileResourceHolder) this.getResourceHolder()).textureHolder.id().orElseGet(MissingTextureAtlasSprite::getLocation);
    }

    @Override
    public Animation getAnimation(String name) {
        return projectileResources.animations().get(name);
    }

    @Override
    public @Nullable AnimationControllerData getAnimationControllerData(String animationControllerName) {
        return projectileResources.controllers().get(animationControllerName);
    }

    @Override
    public boolean isModelPresent() {
        return super.isModelPresent() && projectileResources != null && getResourceHolder().isLoaded();
    }

    @Override
    public float getWidthScale() {
        return 0.7F;
    }

    @Override
    public float getHeightScale() {
        return 0.7F;
    }

    private static class ProjectileResourceHolder extends ResourceHolder {
        private final TextureHolder textureHolder;

        protected ProjectileResourceHolder(ModelRenderTargetLease lease, boolean fallback, ProjectileModelResources projectileResources) {
            super(lease, fallback);
            textureHolder = CustomTextureManager.register(projectileResources.texture(), true);
        }

        @Override
        public boolean isLoaded() {
            return textureHolder.id().isPresent();
        }
    }
}
