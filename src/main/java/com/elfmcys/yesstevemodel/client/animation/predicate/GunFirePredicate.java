package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.compat.swarfare.SWarfareCompat;
import com.elfmcys.yesstevemodel.client.compat.tacz.TACZCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomHumanoidEntity;
import com.elfmcys.yesstevemodel.client.entity.IPreviewEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class GunFirePredicate implements IAnimationPredicate<CustomHumanoidEntity<?>> {
    @Override
    public PlayState test(AnimationEvent<CustomHumanoidEntity<?>> event, ExpressionEvaluator<?> evaluator) {
        LivingEntity entity = event.getAnimatableEntity().getEntity();
        if (entity == null || event.getAnimatableEntity() instanceof IPreviewEntity) {
            return PlayState.STOP;
        }
        if (!entity.swinging && !entity.isUsingItem()) {
            ItemStack mainHandItem = entity.getItemInHand(InteractionHand.MAIN_HAND);
            PlayState result = TACZCompat.playGunOnceAnimation(mainHandItem, event);
            if (result == null) {
                result = SWarfareCompat.playGunOnceAnimation(mainHandItem, event);
            }
            return Objects.requireNonNullElse(result, PlayState.STOP);
        }
        return PlayState.STOP;
    }

    public static boolean available() {
        return TACZCompat.isInstalled() || SWarfareCompat.isInstalled();
    }
}
