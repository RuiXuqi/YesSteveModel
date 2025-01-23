package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.api.IArrowExtraInfo;
import com.elfmcys.yesstevemodel.client.entity.CustomArrowEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.entity.projectile.AbstractArrow;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class ArrowMainPredicate implements IAnimationPredicate<CustomArrowEntity> {
    @Override
    public PlayState test(AnimationEvent<CustomArrowEntity> event, ExpressionEvaluator<?> evaluator) {
        AbstractArrow arrowEntity = event.getAnimatableEntity().getEntity();
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
}
