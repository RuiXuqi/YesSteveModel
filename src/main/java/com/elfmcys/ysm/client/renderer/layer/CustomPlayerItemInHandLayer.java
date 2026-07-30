package com.elfmcys.ysm.client.renderer.layer;

import com.elfmcys.ysm.client.compat.swarfare.SWarfareCompat;
import com.elfmcys.ysm.client.entity.CustomPlayerEntity;
import com.elfmcys.ysm.client.model.PlayerLocator;
import com.elfmcys.ysm.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.ysm.geckolib3.geo.GeoRenderData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CustomPlayerItemInHandLayer extends GeoLayerRenderer<CustomPlayerEntity> {
    private final ItemInHandRenderer itemInHandRenderer;

    public CustomPlayerItemInHandLayer(ItemInHandRenderer itemInHandRenderer) {
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, CustomPlayerEntity animatable, GeoRenderData renderData, int packedLight, int overlay) {
        var entityLivingBaseIn = animatable.getEntity();
        var mainHandItem = entityLivingBaseIn.getMainHandItem();
        if (!mainHandItem.isEmpty()) {
            renderData.modelState.visitLocatorGroup(PlayerLocator.get().rightHandLocator, poseStack, locatorPos -> {
                // if (SlashBladeCompat.isSlashBladeItem(mainHandItem)) {
                    // SlashBladeRender.renderMainhandSlashBlade(entityLivingBaseIn, geoModel, poseStack, bufferIn, packedLightIn, mainHandItem, partialTicks);
                // } else {
                    // TACZCompat.openFlashShellRender(entityLivingBaseIn, mainHandItem);
                    this.renderArmWithItem(entityLivingBaseIn, mainHandItem, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, locatorPos, buffer, packedLight);
                    // TACZCompat.stopFlashShellRender(mainHandItem);
                // }
            });
        }

        var offhandItem = entityLivingBaseIn.getOffhandItem();
        // TODO
//        if (!offhandItem.isEmpty() || !mainHandItem.isEmpty()) {
//            poseStack.pushPose();
//            boolean renderLayersFirst = animatableEntity.renderLayersFirst();
//            if (!geoModel.rightHandBones().isEmpty()) {

//            }
//            if (!geoModel.leftHandBones().isEmpty()) {
//                if (SlashBladeCompat.isSlashBladeItem(offhandItem)) {
//                    SlashBladeRender.renderOffhandSlashBlade(geoModel, poseStack, bufferIn, packedLightIn, offhandItem);
//                } else {
//                    // 卓越前线副手枪械不用这个渲染
//                    if (!SWarfareCompat.isGun(offhandItem)) {
//                        this.renderArmWithItem(geoModel, entityLivingBaseIn, offhandItem, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, poseStack, bufferIn, packedLightIn);
//                    }
//                    if (renderLayersFirst && !offhandItem.isEmpty() && bufferIn instanceof IExtendedBufferSource bufferSource) {
//                        bufferSource.endBatchFixedRenderType();
//                    }
//                }
//            }
//            poseStack.popPose();
//            // TACZ 副手枪械渲染
//            TACZCompat.renderOffsetHand(offhandItem, geoModel, entityLivingBaseIn, poseStack, packedLightIn, partialTicks);
//            // 卓越前线副手枪械渲染
//            SWarfareCompat.renderOffsetHand(offhandItem, geoModel, entityLivingBaseIn, poseStack, packedLightIn, partialTicks);
//        }
    }

    protected void renderArmWithItem(LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext displayContext, HumanoidArm arm, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!itemStack.isEmpty()) {
            boolean isLeftHand = arm == HumanoidArm.LEFT;

            // 渲染默认手部物品
            poseStack.translate(0, -0.0625, -0.1);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            // 卓越前线的枪械需要缩放一下，不然太小了
            if (SWarfareCompat.isGun(itemStack)) {
                poseStack.translate(0.1, 0, 0);
                poseStack.scale(1.25f, 1.25f, 1.25f);
            }
            this.itemInHandRenderer.renderItem(livingEntity, itemStack, displayContext, isLeftHand, poseStack, bufferSource, packedLight);
        }
    }
}
