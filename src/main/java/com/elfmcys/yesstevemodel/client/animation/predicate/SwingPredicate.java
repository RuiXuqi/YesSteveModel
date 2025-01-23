package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionalSwing;
import com.elfmcys.yesstevemodel.client.compat.slashblade.SlashBladeCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.StringUtils;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class SwingPredicate implements IAnimationPredicate<CustomPlayerEntity> {
    @Override
    public PlayState test(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<?> evaluator) {
        Player player = event.getAnimatableEntity().getEntity();
        if (player == null || event.getAnimatableEntity().hasPreviewAnimation()) {
            return PlayState.STOP;
        }

        // 拔刀剑兼容，拔刀剑的使用不受 swing 限制
        if (!player.isSleeping() && SlashBladeCompat.isSlashBladeItem(player.getItemInHand(InteractionHand.MAIN_HAND))) {
            // 起手阻止后续原挥剑动画
            if (event.getCodedController().isAnimFinished()) {
                // 空动画用于重置 PLAY_ONCE 动画
                playAnimation(event, "empty", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
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
        if (player.swinging && !player.isSleeping()) {
            if (player.swingTime == 0) {
                // 空动画用于重置 PLAY_ONCE 动画
                playAnimation(event, "empty", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
            }
            String id = event.getAnimatableEntity().getModelId();
            ConditionalSwing conditionalSwing = ClientModelManager.getModel(id).map(model -> (player.swingingArm == InteractionHand.MAIN_HAND) ? model.conditionManager().getSwingMainhand() : model.conditionManager().getSwingOffhand()).orElse(null);
            if (conditionalSwing != null) {
                String name = conditionalSwing.doTest(player, player.swingingArm);
                if (StringUtils.isNoneBlank(name)) {
                    return playAnimation(event, name, ILoopType.EDefaultLoopTypes.PLAY_ONCE);
                }
            }
            String defaultSwing = (player.swingingArm == InteractionHand.MAIN_HAND) ? "swing_hand" : "swing_offhand";
            return playAnimation(event, defaultSwing, ILoopType.EDefaultLoopTypes.PLAY_ONCE);
        }
        return PlayState.CONTINUE;
    }
}
