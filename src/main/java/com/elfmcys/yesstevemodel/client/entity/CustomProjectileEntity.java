package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.animation.predicate.EmptyPredicate;
import com.elfmcys.yesstevemodel.client.animation.predicate.ParallelPredicate;
import com.elfmcys.yesstevemodel.client.animation.predicate.ProjectileMainPredicate;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.client.model.ProjectileModel;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.*;

public class CustomProjectileEntity extends CustomEntity<Projectile> {
    private ProjectileModel projectileModel;

    public CustomProjectileEntity(Projectile projectile) {
        super(projectile, true);
        registerControllers();
    }

    @SuppressWarnings("unchecked,rawtypes,deprecation")
    private void registerControllers() {
        addAnimationController(new HybridAnimationController(this, PROJECTILE_PRE_MAIN_CONTROLLER, 0, new EmptyPredicate()));
        addAnimationController(new HybridAnimationController(this, PROJECTILE_MAIN_CONTROLLER, 0.1f, new ProjectileMainPredicate()));
        addAnimationController(new HybridAnimationController(this, PROJECTILE_POST_MAIN_CONTROLLER, 0, new EmptyPredicate()));
        for (int i = 0; i < 8; i++) {
            String controllerName = PROJECTILE_PARALLEL_CONTROLLER + i;
            String animationName = String.format("parallel%d", i);
            addAnimationController(new HybridAnimationController<>(this, controllerName, 0,
                    new ParallelPredicate<>(animationName), true));
        }
    }

    @Override
    protected boolean prepareForUpdate() {
        return super.prepareForUpdate() && projectileModel != null;
    }

    /**
     * 当前箭矢没有模型时，由于跳过渲染而不会检查模型更新，此时依赖于异步更新的机制检查。
     */
    @Override
    @SuppressWarnings("deprecation")
    protected boolean onLoadModelContainer(ClientModel newModel, boolean isFallback) {
        projectileModel = isFallback ? null : newModel.projectileModels().get(entity.getType().builtInRegistryHolder().key().location());
        return projectileModel != null;
    }

    @Override
    protected GeoModel getYsmGeoModel() {
        return projectileModel.model();
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation() {
        return projectileModel.texture();
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
        return super.isModelPresent() && projectileModel != null;
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