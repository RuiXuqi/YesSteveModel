package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionalUse;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.StringUtils;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class UsePredicate implements IAnimationPredicate<AnimatableEntity<? extends LivingEntity>> {
    @Override
    public PlayState test(AnimationEvent<AnimatableEntity<? extends LivingEntity>> event, ExpressionEvaluator<?> evaluator) {
        LivingEntity entity = event.getAnimatableEntity().getEntity();
        if (entity == null || event.getAnimatableEntity().hasPreviewAnimation()) {
            return PlayState.STOP;
        }
        if (entity.isUsingItem() && !entity.isSleeping()) {
            if (entity.getTicksUsingItem() == 1) {
                event.getCodedController().forceReload();
            }
            if (entity.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                String id = event.getAnimatableEntity().getModelId();
                ConditionalUse conditionalUse = ClientModelManager.getModel(id).map(model -> model.conditionManager().getUseMainhand()).orElse(null);
                if (conditionalUse != null) {
                    String name = conditionalUse.doTest(entity, InteractionHand.MAIN_HAND);
                    if (StringUtils.isNoneBlank(name)) {
                        return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
                    }
                }
                return playAnimation(event, "use_mainhand", ILoopType.EDefaultLoopTypes.LOOP);
            } else {
                String id = event.getAnimatableEntity().getModelId();
                ConditionalUse conditionalUse = ClientModelManager.getModel(id).map(model -> model.conditionManager().getUseOffhand()).orElse(null);
                if (conditionalUse != null) {
                    String name = conditionalUse.doTest(entity, InteractionHand.OFF_HAND);
                    if (StringUtils.isNoneBlank(name)) {
                        return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
                    }
                }
                return playAnimation(event, "use_offhand", ILoopType.EDefaultLoopTypes.LOOP);
            }
        }
        return PlayState.STOP;
    }
}
