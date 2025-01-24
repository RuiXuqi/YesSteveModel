package com.elfmcys.yesstevemodel.client.compat.parcool;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class ParCoolPredicate implements IAnimationPredicate<CustomPlayerEntity> {
    @Override
    public PlayState test(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<?> evaluator) {
        Player player = event.getAnimatableEntity().getEntity();
        if (player == null || event.getAnimatableEntity().hasPreviewAnimation()) {
            return null;
        }
        String parCoolAnimation = ParCoolCompat.getAnimation(player);
        if (parCoolAnimation != null) {
            String modelId = event.getAnimatableEntity().getModelId();
            Optional<Animation> optional = ClientModelManager.getPlayerAnimation(modelId, parCoolAnimation);
            if (optional.isPresent()) {
                return playAnimation(event, parCoolAnimation);
            }
        }
        return PlayState.STOP;
    }
}
