package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.api.IArrowExtraInfo;
import com.elfmcys.yesstevemodel.client.model.CustomArrowModel;
import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatableModel;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.AnimationBuilder;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.AnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.manager.AnimationData;
import com.elfmcys.yesstevemodel.geckolib3.core.manager.AnimationFactory;
import com.elfmcys.yesstevemodel.geckolib3.resource.GeckoLibCache;
import com.elfmcys.yesstevemodel.geckolib3.util.GeckoLibUtil;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.resources.ResourceLocation;

public class CustomArrowEntity implements IAnimatable<AbstractArrow> {
    private final AnimationFactory factory = GeckoLibUtil.createFactory(this, false);
    private final ResourceLocation mainModel;
    private final ResourceLocation texture;
    private final AbstractArrow arrow;

    public CustomArrowEntity(AbstractArrow arrow, String modelName) {
        this.arrow = arrow;
        ResourceLocation modelId = ModelIdUtil.getModelId(modelName);
        this.mainModel = ModelIdUtil.getArrowId(modelId);
        this.texture = ModelIdUtil.getArrowTextureId(modelId);
    }

    @Override
    @SuppressWarnings("unchecked,rawtypes")
    public void registerControllers(AnimationData data, IAnimatableModel<?> model) {
        data.addAnimationController(new AnimationController(this, model, "main", 2, this::predicateMain));
        for (int i = 0; i < 8; i++) {
            String controllerName = String.format("parallel_%d_controller", i);
            String animationName = String.format("parallel%d", i);
            data.addAnimationController(new AnimationController<>(this, (IAnimatableModel<CustomArrowEntity>) model, controllerName, 0, e -> predicateParallel(e, animationName)));
        }
    }

    public ResourceLocation getMainModel() {
        return mainModel;
    }

    public ResourceLocation getAnimation() {
        if (GeckoLibCache.getInstance().getAnimations().containsKey(this.mainModel)) {
            return mainModel;
        }
        return CustomArrowModel.DEFAULT_ARROW_ANIMATION;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    @Override
    public AbstractArrow getEntity() {
        return arrow;
    }

    public PlayState predicateMain(AnimationEvent<CustomArrowEntity> event) {
        AbstractArrow arrowEntity = event.getAnimatable().getEntity();
        if (arrowEntity == null) {
            return PlayState.STOP;
        }
        if (arrowEntity.isInWater()) {
            return playAnimation(event, "water");
        }
        if (arrowEntity.isOnFire()) {
            return playAnimation(event, "fire");
        }
        if (((IArrowExtraInfo)arrowEntity).isInGround()) {
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

    private static <P extends IAnimatable<?>> PlayState playAnimation(AnimationEvent<P> event, String animationName) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation(animationName, ILoopType.EDefaultLoopTypes.LOOP));
        return PlayState.CONTINUE;
    }
}