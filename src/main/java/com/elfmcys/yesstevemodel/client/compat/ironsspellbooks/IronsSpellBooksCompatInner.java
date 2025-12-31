package com.elfmcys.yesstevemodel.client.compat.ironsspellbooks;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate;
import com.elfmcys.yesstevemodel.client.entity.CustomHumanoidEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

public class IronsSpellBooksCompatInner {
    private static final String PREFIX = "iss:";

    static void addInnerBinding(CtrlBinding binding) {
        binding.clientPlayerVar("iss_animation", ctx -> getAnimation(ctx.entity()));
    }

    static String getAnimation(LivingEntity entity) {
        var animation = ClientMagicData.castingAnimationPlayerLookup.get(entity.getUUID());
        if (animation != null && animation.isActive()) {
            var result = animation.getData().extraData.getOrDefault("name", StringUtils.EMPTY);
            if (result instanceof String str) {
                return str;
            }
            return StringUtils.EMPTY;
        }
        return StringUtils.EMPTY;
    }

    @Nullable
    static PlayState playAnimation(AnimationEvent<CustomHumanoidEntity<?>> event, LivingEntity entity) {
        String animation = getAnimation(entity);
        if (StringUtils.isBlank(animation)) {
            return null;
        }
        // 起手重置动画
        if (event.getCodedController().isAnimFinished()) {
            event.getCodedController().indicateReload();
        }
        String animationName = PREFIX + animation;
        if (event.getAnimatableEntity().getAnimation(animationName) != null) {
            return IAnimationPredicate.playAnimation(event, animationName);
        }
        return null;
    }
}
