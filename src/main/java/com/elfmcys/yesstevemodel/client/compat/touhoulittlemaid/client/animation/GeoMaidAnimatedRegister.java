package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation;

import com.elfmcys.yesstevemodel.client.animation.AnimationState;
import com.elfmcys.yesstevemodel.client.animation.Priority;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.CustomYsmMaidEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.Type;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntitySit;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

import java.util.function.BiPredicate;

public class GeoMaidAnimatedRegister {
    private static final double MIN_SPEED = 0.05;

    public static void registerAnimationState() {
        register("death", ILoopType.EDefaultLoopTypes.PLAY_ONCE, Priority.HIGHEST, (maid, event) -> maid.isDeadOrDying());
        register("riptide", Priority.HIGHEST, (maid, event) -> maid.isAutoSpinAttack());
        register("sleep", Priority.HIGHEST, (maid, event) -> maid.getPose() == Pose.SLEEPING);
        register("swim", Priority.HIGHEST, (maid, event) -> maid.isSwimming());
        register("climb", Priority.HIGHEST, (maid, event) -> maid.getPose() == Pose.SWIMMING && Math.abs(event.getLimbSwingAmount()) > MIN_SPEED);
        register("climbing", Priority.HIGHEST, (maid, event) -> maid.getPose() == Pose.SWIMMING);

        register("ladder_up", Priority.HIGHEST, (maid, event) -> maid.onClimbable() && getVerticalSpeed(maid) > 0);
        register("ladder_stillness", Priority.HIGHEST, (maid, event) -> maid.onClimbable() && getVerticalSpeed(maid) == 0);
        register("ladder_down", Priority.HIGHEST, (maid, event) -> maid.onClimbable() && getVerticalSpeed(maid) < 0);

        // 飞行仅能作用于玩家，故禁用
        // register("fly", Priority.HIGH, (maid, event) -> livingEntity instanceof Player player && player.getAbilities().flying);
        register("elytra_fly", Priority.HIGH, (maid, event) -> maid.getPose() == Pose.FALL_FLYING && maid.isFallFlying());

        // FIXME：女仆的默认骑乘和待命都是 sit，这是否合适？
        register("sit", Priority.HIGH, (maid, event) -> maid.isMaidInSittingPose());

        register("swim_stand", Priority.NORMAL, (maid, event) -> maid.isInWater() && !maid.onGround());
        register("attacked", ILoopType.EDefaultLoopTypes.PLAY_ONCE, Priority.NORMAL, (maid, event) -> maid.hurtTime > 0);
        register("jump", Priority.NORMAL, (maid, event) -> !maid.onGround() && !maid.isInWater());
        register("sneak", Priority.NORMAL, (maid, event) -> maid.onGround() && maid.getPose() == Pose.CROUCHING && Math.abs(event.getLimbSwingAmount()) > MIN_SPEED);
        register("sneaking", Priority.NORMAL, (maid, event) -> maid.onGround() && maid.getPose() == Pose.CROUCHING);

        register("run", Priority.LOW, (maid, event) -> maid.onGround() && maid.isSprinting());
        register("walk", Priority.LOW, (maid, event) -> maid.onGround() && event.getLimbSwingAmount() > MIN_SPEED);

        register("idle", Priority.LOWEST, (maid, event) -> true);
    }

    private static void register(String animationName, ILoopType loopType, int priority, BiPredicate<EntityMaid, AnimationEvent<CustomYsmMaidEntity>> predicate) {
        YsmMaidMainPredicate.register(new AnimationState<>(animationName, loopType, priority, predicate));
    }

    private static void register(String animationName, int priority, BiPredicate<EntityMaid, AnimationEvent<CustomYsmMaidEntity>> predicate) {
        register(animationName, ILoopType.EDefaultLoopTypes.LOOP, priority, predicate);
    }

    private static float getVerticalSpeed(LivingEntity livingEntity) {
        return 20 * (float) (livingEntity.position().y - livingEntity.yo);
    }
}
