package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.entity.CustomVehicleEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.entity.Entity;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class VehicleMovePredicate implements IAnimationPredicate<CustomVehicleEntity> {
    public static final String[] ANIM_LIST = new String[]{"forward", "idle"};

    @Override
    public PlayState test(AnimationEvent<CustomVehicleEntity> event, ExpressionEvaluator<?> evaluator) {
        Entity entity = event.getAnimatableEntity().getEntity();
        if (entity == null) {
            return PlayState.STOP;
        }
        var motion = entity.getDeltaMovement();
        double speed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (speed > 0.05) {
            return playAnimation(event, "forward");
        }
        return playAnimation(event, "idle");
    }
}
