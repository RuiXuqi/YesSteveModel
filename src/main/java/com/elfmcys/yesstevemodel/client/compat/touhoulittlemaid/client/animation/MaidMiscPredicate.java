package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation;

import com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.CustomYsmMaidEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntitySit;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.MaidGameRecordManager;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playLoopAnimation;

public class MaidMiscPredicate implements IAnimationPredicate<CustomYsmMaidEntity> {
    @Override
    public PlayState test(AnimationEvent<CustomYsmMaidEntity> event, ExpressionEvaluator<?> evaluator) {
        EntityMaid maid = event.getAnimatableEntity().getEntity();
        if (maid == null) {
            return PlayState.STOP;
        }
        // 赢棋输棋优先
        if (maid.getVehicle() instanceof EntitySit) {
            MaidGameRecordManager manager = maid.getGameRecordManager();
            if (manager.isWin()) {
                return playLoopAnimation(event, "game_win");
            }
            if (manager.isLost()) {
                return playLoopAnimation(event, "game_lost");
            }
        }
        // 祈求动画
        if (maid.isBegging()) {
            return playLoopAnimation(event, "beg");
        }
        return PlayState.STOP;
    }
}
