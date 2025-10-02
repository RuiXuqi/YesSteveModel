package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.animation.EntityTickStates;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionalUse;
import com.elfmcys.yesstevemodel.client.entity.CustomHumanoidEntity;
import com.elfmcys.yesstevemodel.client.entity.IPreviewEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.LoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.StringUtils;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playCompatAnimation;

public class UsePredicate implements IAnimationPredicate<CustomHumanoidEntity<?>> {
    @Override
    public PlayState test(AnimationEvent<CustomHumanoidEntity<?>> event, ExpressionEvaluator<?> evaluator) {
        LivingEntity entity = event.getAnimatableEntity().getEntity();
        if (entity == null || event.getAnimatableEntity() instanceof IPreviewEntity) {
            return PlayState.STOP;
        }

        int formatVer = event.getAnimatableEntity().getModelContainer().info().formatVer();

        if (entity.isUsingItem() && !entity.isSleeping()) {
            if (entity.getTicksUsingItem() == 1 && event.getAnimatableEntity().getStateTracker().setEntityTickState(EntityTickStates.USING_ITEM)) {
                event.getCodedController().indicateReload();
            }
            var conditionManager = event.getAnimatableEntity().getConditionManager();
            if (entity.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                ConditionalUse conditionalUse = conditionManager.getUseMainhand();
                if (conditionalUse != null) {
                    String name = conditionalUse.doTest(entity, InteractionHand.MAIN_HAND);
                    if (StringUtils.isNoneBlank(name)) {
                        return playCompatAnimation(event, name, LoopType.LOOP, formatVer);
                    }
                }
                return playCompatAnimation(event, "use_mainhand", LoopType.LOOP, formatVer);
            } else {
                ConditionalUse conditionalUse = conditionManager.getUseOffhand();
                if (conditionalUse != null) {
                    String name = conditionalUse.doTest(entity, InteractionHand.OFF_HAND);
                    if (StringUtils.isNoneBlank(name)) {
                        return playCompatAnimation(event, name, LoopType.LOOP, formatVer);
                    }
                }
                return playCompatAnimation(event, "use_offhand", LoopType.LOOP, formatVer);
            }
        }
        return PlayState.STOP;
    }
}
