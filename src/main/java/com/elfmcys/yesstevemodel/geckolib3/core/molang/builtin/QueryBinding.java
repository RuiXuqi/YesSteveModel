package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin;

import com.elfmcys.yesstevemodel.client.event.LocalPlayerTickEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ContextBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.query.*;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.util.EquipmentUtil;
import com.elfmcys.yesstevemodel.util.PersonView;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;

public class QueryBinding extends ContextBinding {
    public static final QueryBinding INSTANCE = new QueryBinding();

    @SuppressWarnings("resource")
    private QueryBinding() {
        function("debug_output", new DebugOutput());

        function("biome_has_all_tags", new BiomeHasAllTags());
        function("biome_has_any_tag", new BiomeHasAnyTag());
        function("relative_block_has_all_tags", new RelativeBlockHasAllTags());
        function("relative_block_has_any_tag", new RelativeBlockHasAnyTag());
        function("is_item_name_any", new ItemNameAny());
        function("equipped_item_all_tags", new EquippedItemAllTags());
        function("equipped_item_any_tag", new EquippedItemAnyTags());
        function("position", new Position());
        function("position_delta", new PositionDelta());

        function("max_durability", new ItemMaxDurability());
        function("remaining_durability", new ItemRemainingDurability());

        var("actor_count", ctx -> ctx.level().getEntityCount());
        var("anim_time", ctx -> ctx.animationContext().animTime());
        // 目前控制器只能同时播放单一动画，所以两个 molang 都是一样的结果
        var("all_animations_finished", ctx -> ctx.animationContext().isAllAnimationsFinished());
        var("any_animation_finished", ctx -> ctx.animationContext().isAnyAnimationFinished());
        var("life_time", ctx -> ctx.animatableEntity().getSeekTime() / 20.0);
        var("head_x_rotation", ctx -> ctx.data().netHeadYaw);
        var("head_y_rotation", ctx -> ctx.data().headPitch);
        var("moon_phase", ctx -> ctx.level().getMoonPhase());
        var("time_of_day", ctx -> MolangUtils.normalizeTime(ctx.level().getDayTime()));
        var("time_stamp", ctx -> ctx.level().getDayTime());

        entityVar("yaw_speed", ctx -> getYawSpeed(ctx.entity()));
        entityVar("cardinal_facing_2d", ctx -> ctx.entity().getDirection().get3DDataValue());
        entityVar("distance_from_camera", ctx -> ctx.mc().gameRenderer.getMainCamera().getPosition().distanceTo(ctx.entity().position()));
        entityVar("eye_target_x_rotation", ctx -> ctx.entity().getViewXRot(ctx.animationEvent().getPartialTick()));
        entityVar("eye_target_y_rotation", ctx -> ctx.entity().getViewYRot(ctx.animationEvent().getPartialTick()));
        entityVar("ground_speed", ctx -> getGroundSpeed(ctx.entity()));
        entityVar("modified_distance_moved", ctx -> ctx.entity().walkDist);
        entityVar("vertical_speed", ctx -> getVerticalSpeed(ctx.entity()));
        entityVar("walk_distance", ctx -> ctx.entity().moveDist);
        entityVar("has_rider", ctx -> ctx.entity().isVehicle());
        entityVar("is_first_person", ctx -> PersonView.getPersonView(ctx) == CameraType.FIRST_PERSON.ordinal());
        entityVar("is_in_water", ctx -> ctx.entity().isInWater());
        entityVar("is_in_water_or_rain", ctx -> ctx.entity().isInWaterRainOrBubble());
        entityVar("is_on_fire", ctx -> ctx.entity().isOnFire());
        entityVar("is_on_ground", ctx -> ctx.entity().onGround());
        entityVar("is_riding", ctx -> ctx.entity().isPassenger());
        entityVar("is_sneaking", ctx -> ctx.entity().onGround() && ctx.entity().getPose() == Pose.CROUCHING);
        entityVar("is_spectator", ctx -> ctx.entity().isSpectator());
        entityVar("is_sprinting", ctx -> ctx.entity().isSprinting());
        entityVar("is_swimming", ctx -> ctx.entity().isSwimming());

        livingEntityVar("body_x_rotation", ctx -> Mth.lerp(ctx.animationEvent().getPartialTick(), ctx.entity().xRotO, ctx.entity().getXRot()));
        livingEntityVar("body_y_rotation", ctx -> Mth.wrapDegrees(Mth.lerp(ctx.animationEvent().getPartialTick(), ctx.entity().yBodyRotO, ctx.entity().yBodyRot)));
        livingEntityVar("health", ctx -> ctx.entity().getHealth());
        livingEntityVar("max_health", ctx -> ctx.entity().getMaxHealth());
        livingEntityVar("hurt_time", ctx -> ctx.entity().hurtTime);
        livingEntityVar("is_eating", ctx -> ctx.entity().getUseItem().getUseAnimation() == UseAnim.EAT);
        livingEntityVar("is_playing_dead", ctx -> ctx.entity().isDeadOrDying());
        livingEntityVar("is_sleeping", ctx -> ctx.entity().isSleeping());
        livingEntityVar("is_using_item", ctx -> ctx.entity().isUsingItem());
        livingEntityVar("item_in_use_duration", ctx -> ctx.entity().getTicksUsingItem() / 20.0);
        livingEntityVar("item_max_use_duration", ctx -> getMaxUseDuration(ctx.entity()) / 20.0);
        livingEntityVar("item_remaining_use_duration", ctx -> ctx.entity().getUseItemRemainingTicks() / 20.0);
        livingEntityVar("equipment_count", ctx -> getEquipmentCount(ctx.entity()));

        playerVar("has_cape", ctx -> hasCape(ctx.entity()));
        playerVar("cape_flap_amount", QueryBinding::getCapeFlapAmount);
        playerVar("player_level", ctx -> ctx.entity().experienceLevel);
        playerVar("is_jumping", ctx -> !ctx.entity().getAbilities().flying && !ctx.entity().isPassenger() && !ctx.entity().onGround() && !ctx.entity().isInWater());
    }

    private static boolean hasCape(AbstractClientPlayer player) {
        return player.isCapeLoaded() && !player.isInvisible() && player.isModelPartShown(PlayerModelPart.CAPE) && player.getCloakTextureLocation() != null;
    }

    private static int getEquipmentCount(LivingEntity entity) {
        int count = 0;
        for (var slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }
            var stack = EquipmentUtil.getEquippedItem(entity, slot);
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static double getMaxUseDuration(LivingEntity player) {
        ItemStack useItem = player.getUseItem();
        if (useItem.isEmpty()) {
            return 0.0;
        } else {
            return useItem.getUseDuration();
        }
    }

    private static float getYawSpeed(Entity entity) {
        if (entity instanceof LocalPlayer) {
            return LocalPlayerTickEvent.getYawSpeed();
        } else {
            return 20 * (entity.getYRot() - entity.yRotO);
        }
    }

    private static float getGroundSpeed(Entity player) {
        Vec3 velocity = player.getDeltaMovement();
        return 20 * Mth.sqrt((float) ((velocity.x * velocity.x) + (velocity.z * velocity.z)));
    }

    private static float getVerticalSpeed(Entity entity) {
        return 20 * (float) (entity.position().y - entity.yo);
    }

    private static float getCapeFlapAmount(IContext<AbstractClientPlayer> ctx) {
        float pPartialTicks = ctx.animationEvent().getPartialTick();
        AbstractClientPlayer pLivingEntity = ctx.entity();

        double d0 = Mth.lerp(pPartialTicks, pLivingEntity.xCloakO, pLivingEntity.xCloak) - Mth.lerp(pPartialTicks, pLivingEntity.xo, pLivingEntity.getX());
        double d1 = Mth.lerp(pPartialTicks, pLivingEntity.yCloakO, pLivingEntity.yCloak) - Mth.lerp(pPartialTicks, pLivingEntity.yo, pLivingEntity.getY());
        double d2 = Mth.lerp(pPartialTicks, pLivingEntity.zCloakO, pLivingEntity.zCloak) - Mth.lerp(pPartialTicks, pLivingEntity.zo, pLivingEntity.getZ());
        float f = pLivingEntity.yBodyRotO + (pLivingEntity.yBodyRot - pLivingEntity.yBodyRotO);
        double d3 = Mth.sin(f * ((float) Math.PI / 180F));
        double d4 = (-Mth.cos(f * ((float) Math.PI / 180F)));
        float f1 = (float) d1 * 10.0F;
        f1 = Mth.clamp(f1, -6.0F, 32.0F);
        float f2 = (float) (d0 * d3 + d2 * d4) * 100.0F;
        f2 = Mth.clamp(f2, 0.0F, 150.0F);
        if (f2 < 0.0F) {
            f2 = 0.0F;
        }

        float f4 = Mth.lerp(pPartialTicks, pLivingEntity.oBob, pLivingEntity.bob);
        f1 = f1 + Mth.sin(Mth.lerp(pPartialTicks, pLivingEntity.walkDistO, pLivingEntity.walkDist) * 6.0F) * 32.0F * f4;
        if (pLivingEntity.isCrouching()) {
            f1 += 25.0F;
        }

        return Mth.clamp((6.0F + f2 / 2.0F + f1) / 108, 0, 1);
    }
}
