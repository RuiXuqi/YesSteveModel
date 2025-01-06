package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.api.IArrowExtraInfo;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.instance.CustomDebugSource;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.AnimationBuilder;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.AnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.info.type.ProjectileType;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.jetbrains.annotations.NotNull;

public class CustomArrowEntity extends AnimatableEntity<AbstractArrow> {
    private String modelId = ModelIdUtil.DEFAULT_MODEL_ID;

    public CustomArrowEntity(AbstractArrow arrow) {
        super(arrow, true);
        getDebugInfo().setEnabled(DebugAnimationKey.TYPE != DebugAnimationKey.DebugType.NONE);
        registerControllers();
    }

    @SuppressWarnings("unchecked,rawtypes")
    public void registerControllers() {
        addAnimationController(new AnimationController(this, "main", 2, this::predicateMain));
        for (int i = 0; i < 8; i++) {
            String controllerName = String.format("parallel_%d_controller", i);
            String animationName = String.format("parallel%d", i);
            addAnimationController(new AnimationController<>(this, controllerName, 0, (event, evaluator) -> predicateParallel(event, animationName)));
        }
    }

    protected void setModelId(String ownerModelId) {
        this.modelId = ownerModelId;
    }

    protected PlayState predicateMain(AnimationEvent<CustomArrowEntity> event, ExpressionEvaluator<AnimationContext<?>> evaluator) {
        AbstractArrow arrowEntity = entity;
        if (arrowEntity == null) {
            return PlayState.STOP;
        }
        if (arrowEntity.isInWater()) {
            return playAnimation(event, "water");
        }
        if (arrowEntity.isOnFire()) {
            return playAnimation(event, "fire");
        }
        if (((IArrowExtraInfo) arrowEntity).isInGround()) {
            return playAnimation(event, "ground");
        } else {
            return playAnimation(event, "air");
        }
    }

    public PlayState predicateParallel(AnimationEvent<CustomArrowEntity> event, String animationName) {
        if (Minecraft.getInstance().isPaused()) {
            return PlayState.STOP;
        }
        return playAnimation(event, animationName);
    }

    private static <P extends AnimatableEntity<?>> PlayState playAnimation(AnimationEvent<P> event, String animationName) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation(animationName, ILoopType.EDefaultLoopTypes.LOOP));
        return PlayState.CONTINUE;
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
    public DebugSource getDebugSource() {
        if(DebugAnimationKey.TYPE != DebugAnimationKey.DebugType.NONE) {
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