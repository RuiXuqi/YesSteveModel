package com.elfmcys.ysm.client.animation.predicate;

import com.elfmcys.ysm.geckolib3.core.PlayState;
import com.elfmcys.ysm.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.ysm.geckolib3.model.AnimatableEntity;
import com.elfmcys.ysm.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.entity.LivingEntity;

public class EmptyPredicate implements IAnimationPredicate<AnimatableEntity<? extends LivingEntity>> {
    public static final EmptyPredicate INSTANCE = new EmptyPredicate();

    @Override
    public PlayState test(AnimationEvent<AnimatableEntity<? extends LivingEntity>> event, ExpressionEvaluator<?> evaluator) {
        return PlayState.STOP;
    }
}
