package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.api.IArrowExtraInfo;
import com.elfmcys.yesstevemodel.client.entity.CustomProjectileEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.entity.projectile.Projectile;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class ProjectileMainPredicate implements IAnimationPredicate<CustomProjectileEntity> {
    public static final String[] ANIM_LIST = new String[]{"water", "ground", "fly", "fire"};

    @Override
    public PlayState test(AnimationEvent<CustomProjectileEntity> event, ExpressionEvaluator<?> evaluator) {
        Projectile projectile = event.getAnimatableEntity().getEntity();
        if (projectile == null) {
            return PlayState.STOP;
        }
        if (projectile.isInWater()) {
            return playAnimation(event, "water");
        }
        if (projectile.isOnFire()) {
            return playAnimation(event, "fire");
        }
        if (projectile instanceof IArrowExtraInfo info && info.isInGround()) {
            return playAnimation(event, "ground");
        } else {
            return playAnimation(event, "air");
        }
    }
}
