package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.predicate.ArrowMainPredicate;
import com.elfmcys.yesstevemodel.client.animation.predicate.ParallelPredicate;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.animation.debug.CustomDebugSource;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.info.type.ProjectileType;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.ARROW_MAIN_CONTROLLER;
import static com.elfmcys.yesstevemodel.util.ControllerUtils.ARROW_PARALLEL_CONTROLLER;

public class CustomArrowEntity extends AnimatableEntity<AbstractArrow> {
    private String modelId = ModelIdUtil.DEFAULT_MODEL_ID;

    public CustomArrowEntity(AbstractArrow arrow) {
        super(arrow, true);
        registerControllers();
    }

    @SuppressWarnings("unchecked,rawtypes")
    public void registerControllers() {
        addAnimationController(new HybridAnimationController(this, ARROW_MAIN_CONTROLLER, 0.1f, new ArrowMainPredicate()));
        for (int i = 0; i < 8; i++) {
            String controllerName = ARROW_PARALLEL_CONTROLLER + i;
            String animationName = String.format("parallel%d", i);
            addAnimationController(new HybridAnimationController<>(this, controllerName, 0,
                    new ParallelPredicate<>(animationName), true));
        }
    }

    protected void setModelId(String ownerModelId) {
        this.modelId = ownerModelId;
    }

    @Override
    public GeoModel getModel() {
        return ClientModelManager.getModel(modelId).map(model -> model.projectileModels().get(ProjectileType.ARROW).model()).orElse(null);
    }

    @Override
    public String getModelId() {
        return modelId;
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation() {
        return ClientModelManager.getModel(modelId).map(model -> model.projectileModels().get(ProjectileType.ARROW).texture()).orElse(MissingTextureAtlasSprite.getLocation());
    }

    @Override
    public Animation getAnimation(String name) {
        return ClientModelManager.getProjectileModel(modelId, ProjectileType.ARROW).map(model -> model.animations().get(name))
                .orElse(null);
    }

    @Override
    public @Nullable IValue getUserFunction(int name) {
        return ClientModelManager.getUserFunction(modelId, name);
    }

    @Override
    public DebugSource getDebugSource() {
        if (DebugAnimationKey.TYPE != DebugAnimationKey.DebugType.NONE) {
            return CustomDebugSource.INSTANCE;
        } else {
            return null;
        }
    }

    @Override
    public boolean isModelPresent() {
        return ClientModelManager.getModel(modelId).map(model -> model.projectileModels().containsKey(ProjectileType.ARROW)).orElse(false);
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