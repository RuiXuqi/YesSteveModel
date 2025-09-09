package com.elfmcys.yesstevemodel.client.compat.parcool;

import com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.entity.IPreviewEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.entity.player.Player;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class ParCoolPredicate implements IAnimationPredicate<CustomPlayerEntity> {
    @Override
    public PlayState test(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<?> evaluator) {
        Player player = event.getAnimatableEntity().getEntity();
        if (player == null || event.getAnimatableEntity() instanceof IPreviewEntity) {
            return null;
        }
        String parCoolAnimation = ParCoolCompat.getAnimation(player);
        if (parCoolAnimation != null) {
            if (event.getAnimatableEntity().getAnimation(parCoolAnimation) != null) {
                return playAnimation(event, parCoolAnimation);
            }
        }
        return PlayState.STOP;
    }
}
