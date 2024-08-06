package com.elfmcys.yesstevemodel.client.compat.tacz;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionTAC;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.AnimationBuilder;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

class TacCompatInner {
    static boolean isGun(ItemStack itemStack) {
        return itemStack.getItem() instanceof IGun;
    }

    static boolean isGrenade(ItemStack itemStack) {
        // TODO 手雷还没有
        return false;
    }

    static void renderOffhandGun(ItemStack heldItem, GeoModelState geoModel, LivingEntity player, PoseStack poseStack, int packedLight, float partialTicks) {
        IGun gun = IGun.getIGunOrNull(heldItem);
        if (gun == null) {
            return;
        }
        TimelessAPI.getCommonGunIndex(gun.getGunId(heldItem)).ifPresent(index -> {
            String weaponType = index.getType();
            ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
            if (isType(weaponType, GunTabType.PISTOL) && !geoModel.tacPistolBones().isEmpty()) {
                RenderUtils.prepMatrixForLocator(poseStack, geoModel.tacPistolBones());
                poseStack.translate(0, -0.125, 0);
                poseStack.scale(0.65f, 0.65f, 0.65f);
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                MultiBufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
                renderer.renderStatic(heldItem, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, player.level(), player.getId());
            }
            if (!isType(weaponType, GunTabType.PISTOL) && !geoModel.tacRifleBones().isEmpty()) {
                RenderUtils.prepMatrixForLocator(poseStack, geoModel.tacRifleBones());
                poseStack.scale(0.65f, 0.65f, 0.65f);
                poseStack.mulPose(Axis.YP.rotationDegrees(-180.0F));
                MultiBufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
                renderer.renderStatic(heldItem, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, player.level(), player.getId());
            }
        });
    }

    static PlayState playGrenadeAnimation(AnimationEvent<CustomPlayerEntity> event, InteractionHand hand) {
        // TODO 手雷还没有
        if (hand == InteractionHand.MAIN_HAND) {
            return playLoopAnimation(event, "tac:mainhand:grenade");
        }
        return playLoopAnimation(event, "tac:offhand:grenade");
    }

    /**
     * tac:idle
     * tac:run
     * tac:walk
     */
    static PlayState playGunMainAnimation(AnimationEvent<CustomPlayerEntity> event, String animationName, ILoopType loopType) {
        String tacName = "tac:" + animationName;
        String modelId = event.getAnimatable().getModelId();
        Optional<Animation> playerAnimation = ClientModelManager.getPlayerAnimation(modelId, tacName);
        if (playerAnimation.isPresent()) {
            return playAnimation(event, tacName, loopType);
        }
        return playAnimation(event, animationName, loopType);
    }

    /**
     * tac:hold:pistol
     * tac:aim:pistol
     * tac:reload:pistol
     * tac:aim_shoot:pistol
     * tac:hold_shoot:pistol
     * tac:run:pistol
     */
    static PlayState playGunHoldAnimation(AnimationEvent<CustomPlayerEntity> event, ItemStack heldItem) {
        IGun gun = IGun.getIGunOrNull(heldItem);
        if (gun == null) {
            return PlayState.STOP;
        }
        Optional<CommonGunIndex> indexOptional = TimelessAPI.getCommonGunIndex(gun.getGunId(heldItem));
        if (indexOptional.isEmpty()) {
            return PlayState.STOP;
        }
        CommonGunIndex gunIndex = indexOptional.get();
        String weaponType = gunIndex.getType();
        Player player = event.getAnimatable().getEntity();
        IGunOperator operator = IGunOperator.fromLivingEntity(player);

        if (!player.isSwimming() && player.getPose() == Pose.SWIMMING) {
            if (Math.abs(event.getLimbSwingAmount()) > 0.05) {
                return getGunTypeAnimation(event, weaponType, "tac:climb:");
            } else {
                return getGunTypeAnimation(event, weaponType, "tac:climbing:");
            }
        }

        float reloadProgress = operator.getSynReloadState().getCountDown();
        if (reloadProgress > 0) {
            if (reloadProgress == 1) {
                event.getController().shouldResetTick = true;
                event.getController().adjustTick(0);
            }
            return getGunTypeAnimation(event, weaponType, "tac:reload:");
        }

        float aimProgress = operator.getSynAimingProgress();
        if (aimProgress > 0) {
            return getGunTypeAnimation(event, weaponType, "tac:aim:");
        } else {
            if (player.onGround() && player.isSprinting()) {
                return getGunTypeAnimation(event, weaponType, "tac:run:");
            }
            return getGunTypeAnimation(event, weaponType, "tac:hold:");
        }
    }

    /**
     * 因为开火没有明确的起止时间，所以单独分一个动画轨道
     */
    static PlayState playGunFireAnimation(AnimationEvent<CustomPlayerEntity> event, ItemStack heldItem) {
        IGun gun = IGun.getIGunOrNull(heldItem);
        if (gun == null) {
            return PlayState.STOP;
        }
        Optional<CommonGunIndex> indexOptional = TimelessAPI.getCommonGunIndex(gun.getGunId(heldItem));
        if (indexOptional.isEmpty()) {
            return PlayState.STOP;
        }

        CommonGunIndex gunIndex = indexOptional.get();
        String weaponType = gunIndex.getType();
        Player player = event.getAnimatable().getEntity();
        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        long fireTick = operator.getSynShootCoolDown();

        if (!player.isSwimming() && player.getPose() == Pose.SWIMMING && Math.abs(event.getLimbSwingAmount()) <= 0.05 && fireTick > 0) {
            return getGunTypeAnimation(event, weaponType, "tac:climbing:fire:", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
        }

        long synMeleeCoolDown = operator.getSynMeleeCoolDown();
        if (synMeleeCoolDown > 0) {
            return getGunTypeAnimation(event, weaponType, "tac:melee:", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
        }

        float aimProgress = operator.getSynAimingProgress();
        if (fireTick > 0) {
            if (aimProgress > 0) {
                return getGunTypeAnimation(event, weaponType, "tac:aim:fire:", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
            } else {
                return getGunTypeAnimation(event, weaponType, "tac:hold:fire:", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
            }
        }
        return playLoopAnimation(event, "empty");
    }

    static void openFlashShellRender(LivingEntity livingEntity) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (livingEntity.equals(player)) {
            MuzzleFlashRender.isSelf = true;
            ShellRender.isSelf = true;
        }
    }

    static void stopFlashShellRender() {
        MuzzleFlashRender.isSelf = false;
        ShellRender.isSelf = false;
    }

    @Nullable
    static ResourceLocation getGunId(ItemStack itemInHand) {
        IGun iGun = IGun.getIGunOrNull(itemInHand);
        if (iGun == null) {
            return null;
        }
        return iGun.getGunId(itemInHand);
    }

    @NotNull
    private static PlayState getGunTypeAnimation(AnimationEvent<CustomPlayerEntity> event, String weaponType, String prefix) {
        return getGunTypeAnimation(event, weaponType, prefix, ILoopType.EDefaultLoopTypes.LOOP);
    }

    @NotNull
    private static PlayState getGunTypeAnimation(AnimationEvent<CustomPlayerEntity> event, String weaponType, String prefix, ILoopType loopType) {
        String modelId = event.getAnimatable().getModelId();
        ConditionTAC conditionTAC = ClientModelManager.getModel(modelId).map(model -> model.conditionManager().getTAC()).orElse(null);
        if (conditionTAC != null) {
            ItemStack stack = event.getAnimatable().getEntity().getMainHandItem();
            String name = conditionTAC.doTest(stack, prefix);
            if (StringUtils.isNoneBlank(name)) {
                return playAnimation(event, name, loopType);
            }
        }
        if (isType(weaponType, GunTabType.PISTOL)) {
            return playAnimation(event, prefix + "pistol", loopType);
        }
        if (isType(weaponType, GunTabType.RPG)) {
            return playAnimation(event, prefix + "rpg", loopType);
        }
        return playAnimation(event, prefix + "rifle", loopType);
    }

    @NotNull
    private static PlayState playLoopAnimation(AnimationEvent<?> event, String animationName) {
        return playAnimation(event, animationName, ILoopType.EDefaultLoopTypes.LOOP);
    }

    @NotNull
    private static PlayState playAnimation(AnimationEvent<?> event, String animationName, ILoopType loopType) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation(animationName, loopType));
        return PlayState.CONTINUE;
    }

    private static boolean isType(String type, GunTabType tabType) {
        return type.equals(tabType.name().toLowerCase(Locale.ENGLISH));
    }
}
