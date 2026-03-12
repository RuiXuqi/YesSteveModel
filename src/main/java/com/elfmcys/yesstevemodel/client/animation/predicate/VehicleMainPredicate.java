package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.entity.CustomVehicleEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.entity.Entity;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class VehicleMainPredicate implements IAnimationPredicate<CustomVehicleEntity> {
    public static final String[] ANIM_LIST = new String[]{"water", "ground", "fly"};

    @Override
    public PlayState test(AnimationEvent<CustomVehicleEntity> event, ExpressionEvaluator<?> evaluator) {
        Entity entity = event.getAnimatableEntity().getEntity();
        if (entity == null) {
            return PlayState.STOP;
        }
        if (entity.isInWater()) {
            return playAnimation(event, "water");
        }
        if (entity.onGround()) {
            return playAnimation(event, "ground");
        }
        return playAnimation(event, "fly");
    }
}
