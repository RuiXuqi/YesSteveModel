package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.CustomYsmMaidEntity;
import com.elfmcys.yesstevemodel.client.entity.IPreviewEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import com.github.tartaricacid.touhoulittlemaid.api.client.render.MaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playLoopAnimation;

public class MaidStatuePredicate implements IAnimationPredicate<CustomYsmMaidEntity> {
    @Override
    public PlayState test(AnimationEvent<CustomYsmMaidEntity> event, ExpressionEvaluator<?> evaluator) {
        EntityMaid maid = event.getAnimatableEntity().getEntity();
        if (maid == null || event.getAnimatableEntity() instanceof IPreviewEntity) {
            return PlayState.STOP;
        }
        // 检查女仆是否是雕像或者手办状态，如果是，那么播放对应动画
        if (maid.renderState == MaidRenderState.STATUE) {
            return playLoopAnimation(event, "statue");
        }
        if (maid.renderState == MaidRenderState.GARAGE_KIT) {
            return playLoopAnimation(event, "garage_kit");
        }
        return PlayState.STOP;
    }
}
