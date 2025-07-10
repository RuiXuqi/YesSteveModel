package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.EntityTickStates;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionalUse;
import com.elfmcys.yesstevemodel.client.entity.IPreviewEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.LoopType;
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
        if (entity == null || event.getAnimatableEntity() instanceof IPreviewEntity) {
            return PlayState.STOP;
        }
        if (entity.isUsingItem() && !entity.isSleeping()) {
            if (entity.getTicksUsingItem() == 1 && event.getAnimatableEntity().getStateTracker().setEntityTickState(EntityTickStates.USING_ITEM)) {
                event.getCodedController().forceReload();
            }
            if (entity.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                String id = event.getAnimatableEntity().getModelId();
                ConditionalUse conditionalUse = ClientModelManager.getModel(id).map(model -> model.conditionManager().getUseMainhand()).orElse(null);
                if (conditionalUse != null) {
                    String name = conditionalUse.doTest(entity, InteractionHand.MAIN_HAND);
                    if (StringUtils.isNoneBlank(name)) {
                        return playAnimation(event, name, LoopType.LOOP);
                    }
                }
                return playAnimation(event, "use_mainhand", LoopType.LOOP);
            } else {
                String id = event.getAnimatableEntity().getModelId();
                ConditionalUse conditionalUse = ClientModelManager.getModel(id).map(model -> model.conditionManager().getUseOffhand()).orElse(null);
                if (conditionalUse != null) {
                    String name = conditionalUse.doTest(entity, InteractionHand.OFF_HAND);
                    if (StringUtils.isNoneBlank(name)) {
                        return playAnimation(event, name, LoopType.LOOP);
                    }
                }
                return playAnimation(event, "use_offhand", LoopType.LOOP);
            }
        }
        return PlayState.STOP;
    }
}
