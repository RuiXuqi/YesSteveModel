package com.elfmcys.yesstevemodel.client.animation.molang;

import com.elfmcys.yesstevemodel.client.animation.molang.functions.ArmorCheck;
import com.elfmcys.yesstevemodel.client.animation.molang.functions.HandItemCheck;
import com.elfmcys.yesstevemodel.client.animation.molang.functions.RideCheck;
import com.elfmcys.yesstevemodel.client.compat.carryon.CarryOnCompat;
import com.elfmcys.yesstevemodel.client.compat.parcool.ParCoolCompat;
import com.elfmcys.yesstevemodel.client.compat.slashblade.SlashBladeCompat;
import com.elfmcys.yesstevemodel.client.compat.swem.SwemCompat;
import com.elfmcys.yesstevemodel.client.compat.tacz.TACZCompat;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ContextBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

public class CtrlBinding extends ContextBinding {
    public static final CtrlBinding INSTANCE = new CtrlBinding();
    private static final double MIN_SPEED = 0.05;

    @SuppressWarnings("resource")
    private CtrlBinding() {
        // 主动画的
        livingEntityVar("death", ctx -> ctx.entity().isDeadOrDying());
        livingEntityVar("riptide", ctx -> ctx.entity().isAutoSpinAttack());
        livingEntityVar("sleep", ctx -> ctx.entity().getPose() == Pose.SLEEPING);
        livingEntityVar("swim", ctx -> ctx.entity().isSwimming());
        livingEntityVar("climb", ctx -> !ctx.entity().isSwimming() && ctx.entity().getPose() == Pose.SWIMMING && isMoving(ctx.entity()));
        livingEntityVar("climbing", ctx -> !ctx.entity().isSwimming() && ctx.entity().getPose() == Pose.SWIMMING && !isMoving(ctx.entity()));

        livingEntityVar("ladder_up", ctx -> ctx.entity().onClimbable() && getVerticalSpeed(ctx.entity()) > 0);
        livingEntityVar("ladder_stillness", ctx -> ctx.entity().onClimbable() && getVerticalSpeed(ctx.entity()) == 0);
        livingEntityVar("ladder_down", ctx -> ctx.entity().onClimbable() && getVerticalSpeed(ctx.entity()) < 0);

        playerVar("fly", ctx -> ctx.entity().getAbilities().flying);
        livingEntityVar("elytra_fly", ctx -> ctx.entity().getPose() == Pose.FALL_FLYING && ctx.entity().isFallFlying());

        livingEntityVar("swim_stand", ctx -> ctx.entity().isInWater() && !ctx.entity().isSwimming() && !ctx.entity().onGround());
        livingEntityVar("attacked", ctx -> ctx.entity().hurtTime > 0);
        livingEntityVar("jump", ctx -> !ctx.entity().onGround() && !ctx.entity().isInWater());
        livingEntityVar("sneak", ctx -> ctx.entity().onGround() && ctx.entity().getPose() == Pose.CROUCHING && isMoving(ctx.entity()));
        livingEntityVar("sneaking", ctx -> ctx.entity().onGround() && ctx.entity().getPose() == Pose.CROUCHING && !isMoving(ctx.entity()));

        livingEntityVar("run", ctx -> ctx.entity().getPose() == Pose.STANDING && ctx.entity().onGround() && ctx.entity().isSprinting());
        livingEntityVar("walk", ctx -> ctx.entity().getPose() == Pose.STANDING && ctx.entity().onGround() && !ctx.entity().isSprinting() && isMoving(ctx.entity()));
        livingEntityVar("idle", ctx -> ctx.entity().getPose() == Pose.STANDING && ctx.entity().onGround() && !ctx.entity().isSprinting() && !isMoving(ctx.entity()));

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
    }

    private static boolean isMoving(LivingEntity entity) {
        float partialTick = Minecraft.getInstance().getPartialTick();
        float limbSwingAmount = entity.walkAnimation.speed(partialTick);
        return Math.abs(limbSwingAmount) > MIN_SPEED;
    }

    private static float getVerticalSpeed(LivingEntity entity) {
        return 20 * (float) (entity.position().y - entity.yo);
    }
}
