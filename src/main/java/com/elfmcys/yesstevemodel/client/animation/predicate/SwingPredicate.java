package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.EntityTickStates;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionalSwing;
import com.elfmcys.yesstevemodel.client.compat.slashblade.SlashBladeCompat;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.StringUtils;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class SwingPredicate implements IAnimationPredicate<AnimatableEntity<? extends LivingEntity>> {
    @Override
    public PlayState test(AnimationEvent<AnimatableEntity<? extends LivingEntity>> event, ExpressionEvaluator<?> evaluator) {
        LivingEntity entity = event.getAnimatableEntity().getEntity();
        if (entity == null || event.getAnimatableEntity().hasPreviewAnimation()) {
            return PlayState.STOP;
        }

        // 拔刀剑兼容，拔刀剑的使用不受 swing 限制
        if (!entity.isSleeping() && SlashBladeCompat.isSlashBladeItem(entity.getItemInHand(InteractionHand.MAIN_HAND))) {
            // 起手阻止后续原挥剑动画
            if (event.getCodedController().isAnimFinished(event.renderTicks)) {
                // 重置动画
                event.getCodedController().forceReload();
            }
            String animationName = SlashBladeCompat.getAnimationName(event);
            if (StringUtils.isNoneBlank(animationName)) {
                String id = event.getAnimatableEntity().getModelId();
                return ClientModelManager.getModel(id).map(clientModel -> {
                    if (clientModel.animations().containsKey(animationName)) {
                        return playAnimation(event, animationName, ILoopType.EDefaultLoopTypes.PLAY_ONCE);
                    }
                    return PlayState.CONTINUE;
                }).orElse(PlayState.STOP);
            }
        }

        // 其他情况
        if (entity.swinging && !entity.isSleeping()) {
            if (entity.swingTime == 0 && event.getAnimatableEntity().setEntityTickState(EntityTickStates.SWING)) {
                // swing 开始时重置动画
                event.getCodedController().forceReload();
            }
            String id = event.getAnimatableEntity().getModelId();
            ConditionalSwing conditionalSwing = ClientModelManager.getModel(id).map(model -> (entity.swingingArm == InteractionHand.MAIN_HAND) ? model.conditionManager().getSwingMainhand() : model.conditionManager().getSwingOffhand()).orElse(null);
            if (conditionalSwing != null) {
                String name = conditionalSwing.doTest(entity, entity.swingingArm);
                if (StringUtils.isNoneBlank(name)) {
                    return playAnimation(event, name, ILoopType.EDefaultLoopTypes.PLAY_ONCE);
                }
            }
            String defaultSwing = (entity.swingingArm == InteractionHand.MAIN_HAND) ? "swing_hand" : "swing_offhand";
            return playAnimation(event, defaultSwing, ILoopType.EDefaultLoopTypes.PLAY_ONCE);
        }
        return PlayState.CONTINUE;
    }
}
