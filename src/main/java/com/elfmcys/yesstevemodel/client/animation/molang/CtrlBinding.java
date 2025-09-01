package com.elfmcys.yesstevemodel.client.animation.molang;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapability;
import com.elfmcys.yesstevemodel.client.animation.Priority;
import com.elfmcys.yesstevemodel.client.animation.molang.functions.*;
import com.elfmcys.yesstevemodel.client.compat.backpack.sophisticated.SophisticatedCompat;
import com.elfmcys.yesstevemodel.client.compat.carryon.CarryOnCompat;
import com.elfmcys.yesstevemodel.client.compat.create.CreateCompat;
import com.elfmcys.yesstevemodel.client.compat.parcool.ParCoolCompat;
import com.elfmcys.yesstevemodel.client.compat.slashblade.SlashBladeCompat;
import com.elfmcys.yesstevemodel.client.compat.swem.SwemCompat;
import com.elfmcys.yesstevemodel.client.compat.tacz.TACZCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.entity.IPreviewEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimationState;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ContextBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.util.ControllerUtils;
import com.elfmcys.yesstevemodel.util.LazyValue;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

public class CtrlBinding extends ContextBinding {
    public static final LazyValue<CtrlBinding> INSTANCE = new LazyValue<>(CtrlBinding::new);

    public static final int STATE_CONTINUE = 2;
    public static final int STATE_STOP = 3;
    public static final int STATE_PAUSE = 4;
    public static final int STATE_BYPASS = 5;

    public static final int LOOP = 10;
    public static final int PLAY_ONCE = 11;
    public static final int HOLD_ON_LAST_FRAME = 12;

    private static ReferenceArrayList<Condition>[] DATA;
    private static final float MIN_SPEED = 0.05f;

    private CtrlBinding() {
        // 主动画的
        register("death", Priority.HIGHEST, LivingEntity::isDeadOrDying);
        register("riptide", Priority.HIGHEST, LivingEntity::isAutoSpinAttack);
        register("sleep", Priority.HIGHEST, entity -> entity.getPose() == Pose.SLEEPING);
        register("swim", Priority.HIGHEST, Entity::isSwimming);
        register("climb", Priority.HIGHEST, entity -> entity.getPose() == Pose.SWIMMING && isMoving(entity));
        register("climbing", Priority.HIGHEST, entity -> entity.getPose() == Pose.SWIMMING);

        register("ladder_up", Priority.HIGHEST, entity -> entity.onClimbable() && getVerticalSpeed(entity) > 0);
        register("ladder_stillness", Priority.HIGHEST, entity -> entity.onClimbable() && getVerticalSpeed(entity) == 0);
        register("ladder_down", Priority.HIGHEST, entity -> entity.onClimbable() && getVerticalSpeed(entity) < 0);

        register("fly", Priority.HIGH, CtrlBinding::isFlying);
        register("elytra_fly", Priority.HIGH, entity -> entity.getPose() == Pose.FALL_FLYING && entity.isFallFlying());

        register("swim_stand", Priority.NORMAL, entity -> entity.isInWater() && !entity.onGround());
        register("attacked", Priority.NORMAL, entity -> entity.hurtTime > 0);
        register("jump", Priority.NORMAL, entity -> !entity.onGround() && !entity.isInWater());
        register("sneak", Priority.NORMAL, entity -> entity.onGround() && entity.getPose() == Pose.CROUCHING && isMoving(entity));
        register("sneaking", Priority.NORMAL, entity -> entity.onGround() && entity.getPose() == Pose.CROUCHING);

        register("run", Priority.LOW, entity -> entity.onGround() && entity.isSprinting());
        register("walk", Priority.LOW, entity -> entity.onGround() && isMoving(entity));

        register("idle", Priority.LOWEST, entity -> true);

        // 轮盘动画预测
        var("playing_extra_animation", CtrlBinding::checkExtraAnimationState);

        // 条件动画的
        function("hold", HandItemCheck.holdCheck());
        function("swing", HandItemCheck.swingCheck());
        function("use", HandItemCheck.useCheck());
        function("armor", ArmorCheck.armorCheck());
        function("ride", RideCheck.rideCheck());

        // 模组的
        CarryOnCompat.addBinding(this);
        TACZCompat.addBinding(this);
        SwemCompat.addBinding(this);
        ParCoolCompat.addBinding(this);
        SlashBladeCompat.addBinding(this);
        SophisticatedCompat.addBinding(this);
        CreateCompat.addBinding(this);

        // 硬编码预测函数用
        constValue("state_continue", STATE_CONTINUE);
        constValue("state_stop", STATE_STOP);
        constValue("state_pause", STATE_PAUSE);
        constValue("state_bypass", STATE_BYPASS);

        constValue("loop", LOOP);
        constValue("play_once", PLAY_ONCE);
        constValue("hold_on_last_frame", HOLD_ON_LAST_FRAME);

        function("set_animation", new SetAnimation());
        function("reset", new ResetController());
        function("indicate_reload", new IndicateReload());
    }

    private static boolean checkExtraAnimationState(IContext<Object> ctx) {
        if (ctx.animatableEntity() instanceof CustomPlayerEntity entity && entity.isPlayingExtraAnimation()) {
            return entity.getCodedAnimationStates(ControllerUtils.CAP_CONTROLLER) != AnimationState.IDLE;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void register(String name, int priority, Predicate<IContext<LivingEntity>> predicate) {
        if (DATA == null) {
            DATA = new ReferenceArrayList[Priority.LOWEST + 1];
            for (int i = 0; i < DATA.length; i++) {
                DATA[i] = new ReferenceArrayList<>(6);
            }
        }
        Condition condition = new Condition(name, priority, predicate);
        DATA[priority].add(condition);
        livingEntityVar(name, ctx -> testCondition(name, ctx));
    }

    private void register(String name, int priority, LivingEntityPredicate predicate) {
        register(name, priority, (Predicate<IContext<LivingEntity>>) predicate);
    }

    private static boolean testCondition(String name, IContext<LivingEntity> context) {
        LivingEntity entity = context.entity();

        if (context.animatableEntity() instanceof IPreviewEntity) {
            return false;
        }

        // 跑酷
        if (entity instanceof Player player) {
            boolean parcool = ParCoolCompat.hasAnimation(player);
            if (parcool) {
                return false;
            }
        }

        // 载具
        Entity vehicle = entity.getVehicle();
        if (vehicle != null && vehicle.isAlive()) {
            return false;
        }

        for (int i = Priority.HIGHEST; i <= Priority.LOWEST; i++) {
            for (Condition condition : DATA[i]) {
                if (condition.predicate().test(context)) {
                    return condition.name().equals(name);
                }
            }
        }
        return false;
    }

    private static boolean isMoving(LivingEntity entity) {
        float partialTick = Minecraft.getInstance().getPartialTick();
        float limbSwingAmount = entity.walkAnimation.speed(partialTick);
        return Math.abs(limbSwingAmount) > MIN_SPEED;
    }

    private static float getVerticalSpeed(LivingEntity entity) {
        return 20 * (float) (entity.position().y - entity.yo);
    }

    private static boolean isFlying(IContext<LivingEntity> ctx) {
        if (ctx.animatableEntity() instanceof PlayerAnimatableCapability cap) {
            return cap.getStateTracker().isFlying();
        } else if (ctx.entity() instanceof Player player) {
            return player.getAbilities().flying;
        }
        return false;
    }

    private record Condition(String name, int priority, Predicate<IContext<LivingEntity>> predicate) {
    }

    private interface LivingEntityPredicate extends Predicate<IContext<LivingEntity>> {
        boolean testLivingEntity(LivingEntity entity);

        default boolean test(IContext<LivingEntity> context) {
            return testLivingEntity(context.entity());
        }
    }
}
