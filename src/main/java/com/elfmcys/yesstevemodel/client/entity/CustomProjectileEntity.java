package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.client.model.ProjectileModel;
import com.elfmcys.yesstevemodel.client.texture.CustomTextureManager;
import com.elfmcys.yesstevemodel.client.texture.TextureHolder;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomProjectileEntity extends CustomEntity<Projectile> {
    private ProjectileModel projectileModel;

    public CustomProjectileEntity(Projectile projectile) {
        super(projectile, true);
    }

    @Override
    protected void onSetupAnimationController() {
        if (projectileModel != null) {
            projectileModel.controllerFactory().accept(this);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected @Nullable ResourceHolder createResourceHolder(ClientModel model, boolean isFallback) {
        if (!isFallback) {
            var projectileModel = model.projectileModels().get(entity.getType().builtInRegistryHolder().key().location());
            if (projectileModel != null) {
                return new ProjectileResourceHolder(model, false, projectileModel);
            }
        }
        return null;
    }

    /**
     * 当前箭矢没有模型时，由于跳过渲染而不会检查模型更新，此时依赖于异步更新的机制检查。
     */
    @Override
    @SuppressWarnings("deprecation")
    protected void onLoadModelContainer(ClientModel newModel) {
        super.onLoadModelContainer(newModel);
        projectileModel = newModel.projectileModels().get(entity.getType().builtInRegistryHolder().key().location());
    }

    @Override
    public void resetModelContainer() {
        super.resetModelContainer();
        projectileModel = null;
    }

    @Override
    protected GeoModel getYsmGeoModel() {
        return projectileModel.model();
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation() {
        return ((ProjectileResourceHolder) this.getResourceHolder()).textureHolder.id().orElseGet(MissingTextureAtlasSprite::getLocation);
    }

    @Override
    public Animation getAnimation(String name) {
        return projectileModel.animations().get(name);
    }

    @Override
    public @Nullable AnimationControllerData getAnimationControllerData(String animationControllerName) {
        return projectileModel.controllers().get(animationControllerName);
    }

    @Override
    public boolean isModelPresent() {
        return super.isModelPresent() && projectileModel != null && getResourceHolder().isLoaded();
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

        protected ProjectileResourceHolder(ClientModel model, boolean fallback, ProjectileModel projectileModel) {
            super(model, fallback);
            textureHolder = CustomTextureManager.register(projectileModel.texture(), true);
        }

        @Override
        public boolean isLoaded() {
            return textureHolder.id().isPresent();
        }
    }
}