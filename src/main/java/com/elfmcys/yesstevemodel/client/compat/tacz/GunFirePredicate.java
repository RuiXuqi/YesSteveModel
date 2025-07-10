package com.elfmcys.yesstevemodel.client.compat.tacz;

import com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate;
import com.elfmcys.yesstevemodel.client.entity.IPreviewEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class GunFirePredicate implements IAnimationPredicate<AnimatableEntity<? extends LivingEntity>> {
    @Override
    public PlayState test(AnimationEvent<AnimatableEntity<? extends LivingEntity>> event, ExpressionEvaluator<?> evaluator) {
        LivingEntity entity = event.getAnimatableEntity().getEntity();
        if (entity == null || event.getAnimatableEntity() instanceof IPreviewEntity) {
            return PlayState.STOP;
        }
        if (!entity.swinging && !entity.isUsingItem()) {
            ItemStack mainHandItem = entity.getItemInHand(InteractionHand.MAIN_HAND);
            return TACZCompat.playGunFireAnimation(mainHandItem, event);
        }
        return PlayState.STOP;
    }
}
