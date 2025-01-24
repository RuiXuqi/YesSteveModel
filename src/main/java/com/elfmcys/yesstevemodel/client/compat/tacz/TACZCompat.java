package com.elfmcys.yesstevemodel.client.compat.tacz;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.TACZ_FIRE_CONTROLLER;


public class TACZCompat {
    private static final String MOD_ID = "tacz";
    private static boolean INSTALLED = false;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
        if (INSTALLED) {
            TacCompatInner.registerEvent();
        }
    }

    public static void addTaczPredicate(CustomPlayerEntity entity) {
        if (INSTALLED) {
            entity.addAnimationController(new HybridAnimationController(entity, TACZ_FIRE_CONTROLLER, 0, new GunFirePredicate()));
        }
    }

    public static void addBinding(CtrlBinding ctrlBinding) {
        if (isInstalled()) {
            TacCtrlBinding.addInnerBinding(ctrlBinding);
        } else {
            addEmptyBinding(ctrlBinding);
        }
    }

    /**
     * 没有安装此模组时，这些 molang 应该存在，否则会报错
     */
    private static void addEmptyBinding(CtrlBinding binding) {
        binding.playerVar("tac_hold_gun", ctx -> false);
        binding.playerVar("tac_gun_type", ctx -> StringUtils.EMPTY);
        binding.playerVar("tac_gun_id", ctx -> StringUtils.EMPTY);
        binding.playerVar("tac_is_fire", ctx -> false);
        binding.playerVar("tac_is_aim", ctx -> false);
        binding.playerVar("tac_is_reload", ctx -> false);
        binding.playerVar("tac_is_melee", ctx -> false);
        binding.playerVar("tac_is_draw", ctx -> false);
    }

    public static void renderOffsetHand(ItemStack offhandItem, GeoModelState geoModel, LivingEntity livingEntity, PoseStack poseStack, int packedLight, float partialTicks) {
        if (isInstalled() && TacCompatInner.isGun(offhandItem)) {
            poseStack.pushPose();
            TacCompatInner.renderOffhandGun(offhandItem, geoModel, livingEntity, poseStack, packedLight, partialTicks);
            poseStack.popPose();
        }
    }

    @Nullable
    public static PlayState playGunMainAnimation(Player player, AnimationEvent<CustomPlayerEntity> event, String animationName, ILoopType loopType) {
        if (isInstalled() && TacCompatInner.isGun(player.getMainHandItem())) {
            return TacCompatInner.playGunMainAnimation(event, animationName, loopType);
        }
        return null;
    }

    @Nullable
    public static PlayState playGunHoldAnimation(ItemStack mainHandItem, AnimationEvent<CustomPlayerEntity> event) {
        if (isInstalled() && TacCompatInner.isGun(mainHandItem)) {
            return TacCompatInner.playGunHoldAnimation(event, mainHandItem);
        }
        return null;
    }

    @Nullable
    public static PlayState playGunFireAnimation(ItemStack mainHandItem, AnimationEvent<CustomPlayerEntity> event) {
        if (isInstalled() && TacCompatInner.isGun(mainHandItem)) {
            return TacCompatInner.playGunOnceAnimation(event, mainHandItem);
        }
        return PlayState.STOP;
    }

    public static void openFlashShellRender(LivingEntity livingEntity, ItemStack mainHandItem) {
        if (isInstalled() && TacCompatInner.isGun(mainHandItem)) {
            TacCompatInner.openFlashShellRender(livingEntity);
        }
    }

    public static void stopFlashShellRender(ItemStack mainHandItem) {
        if (isInstalled() && TacCompatInner.isGun(mainHandItem)) {
            TacCompatInner.stopFlashShellRender();
        }
    }

    @Nullable
    public static ResourceLocation getGunId(ItemStack stack) {
        if (isInstalled()) {
            return TacCompatInner.getGunId(stack);
        }
        return null;
    }

    private static boolean isInstalled() {
        return INSTALLED;
    }
}
