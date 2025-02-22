package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation;

import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.Type;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntitySit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playLoopAnimation;

public class MaidVehiclePredicate {
    @Nullable
    public static PlayState getMaidVehicleAnimation(AnimationEvent<AnimatableEntity<? extends LivingEntity>> event, LivingEntity entity, Entity vehicle) {
        if (vehicle instanceof EntitySit sit) {
            String joyType = sit.getJoyType();
            if (joyType.equals(Type.GOMOKU.getTypeName())) {
                return playLoopAnimation(event, "gomoku");
            } else if (joyType.equals(Type.BOOKSHELF.getTypeName())) {
                return playLoopAnimation(event, "bookshelf");
            } else if (joyType.equals(Type.COMPUTER.getTypeName())) {
                return playLoopAnimation(event, "computer");
            } else if (joyType.equals(Type.KEYBOARD.getTypeName())) {
                return playLoopAnimation(event, "keyboard");
            } else if (joyType.equals(Type.ON_HOME_MEAL.getTypeName())) {
                return playLoopAnimation(event, "picnic");
            }
        }
        if (vehicle instanceof EntityChair) {
            return playLoopAnimation(event, "chair");
        }
        return null;
    }
}
