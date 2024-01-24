package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.client.instance.CustomPlayerInstance;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CustomPlayerItemInHandLayer extends GeoLayerRenderer<CustomPlayerInstance> {
    private final ItemInHandRenderer itemInHandRenderer;

    public CustomPlayerItemInHandLayer(ItemInHandRenderer itemInHandRenderer) {
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn, CustomPlayerInstance instance, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        LivingEntity entityLivingBaseIn = instance.getAnimatable().getEntity();
        GeoModelState geoModel = instance.getAnimatableModel().getCurrentModel();
        if(geoModel == null) {
            return;
        }
        ItemStack offhandItem = entityLivingBaseIn.getOffhandItem();
        ItemStack mainHandItem = entityLivingBaseIn.getMainHandItem();
        if (!offhandItem.isEmpty() || !mainHandItem.isEmpty()) {
            poseStack.pushPose();
            if (!geoModel.rightHandBones().isEmpty()) {
                this.renderArmWithItem(geoModel, entityLivingBaseIn, mainHandItem, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, poseStack, bufferIn, packedLightIn);
            }
            if (!geoModel.leftHandBones().isEmpty()) {
                this.renderArmWithItem(geoModel, entityLivingBaseIn, offhandItem, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, poseStack, bufferIn, packedLightIn);
            }
            poseStack.popPose();
        }
    }

    protected void renderArmWithItem(GeoModelState geoModel, LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext displayContext, HumanoidArm arm, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        if (!itemStack.isEmpty()) {
            poseStack.pushPose();
            translateToHand(arm, poseStack, geoModel);
            poseStack.translate(0, -0.0625, -0.1);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            boolean isLeftHand = arm == HumanoidArm.LEFT;
            this.itemInHandRenderer.renderItem(livingEntity, itemStack, displayContext, isLeftHand, poseStack, bufferSource, light);
            poseStack.popPose();
        }
    }

    protected void translateToHand(HumanoidArm arm, PoseStack poseStack, GeoModelState geoModel) {
        if (arm == HumanoidArm.LEFT) {
            int size = geoModel.leftHandBones().size();
            for (int i = 0; i < size - 1; i++) {
                RenderUtils.prepMatrixForBone(poseStack, geoModel.leftHandBones().get(i));
            }
            IBone lastBone = geoModel.leftHandBones().get(size - 1);
            RenderUtils.translateMatrixToBone(poseStack, lastBone);
            RenderUtils.translateToPivotPoint(poseStack, lastBone);
            RenderUtils.rotateMatrixAroundBone(poseStack, lastBone);
            RenderUtils.scaleMatrixForBone(poseStack, lastBone);
        } else {
            int size = geoModel.rightHandBones().size();
            for (int i = 0; i < size - 1; i++) {
                RenderUtils.prepMatrixForBone(poseStack, geoModel.rightHandBones().get(i));
            }
            IBone lastBone = geoModel.rightHandBones().get(size - 1);
            RenderUtils.translateMatrixToBone(poseStack, lastBone);
            RenderUtils.translateToPivotPoint(poseStack, lastBone);
            RenderUtils.rotateMatrixAroundBone(poseStack, lastBone);
            RenderUtils.scaleMatrixForBone(poseStack, lastBone);
        }
    }
}
