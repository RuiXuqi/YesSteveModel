package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.api.IExtendedBufferSource;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.compat.slashblade.SlashBladeCompat;
import com.elfmcys.yesstevemodel.client.compat.slashblade.SlashBladeRender;
import com.elfmcys.yesstevemodel.client.compat.tacz.TACZCompat;
import com.elfmcys.yesstevemodel.client.instance.CustomPlayerInstance;
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
        if (geoModel == null) {
            return;
        }
        ItemStack offhandItem = entityLivingBaseIn.getOffhandItem();
        ItemStack mainHandItem = entityLivingBaseIn.getMainHandItem();
        if (!offhandItem.isEmpty() || !mainHandItem.isEmpty()) {
            poseStack.pushPose();
            boolean renderLayersFirst = ClientModelManager.getModel(instance.getModelId()).map(m -> m.modelInfo().properties().renderLayersFirst()).orElse(false);
            if (!geoModel.rightHandBones().isEmpty()) {
                if (SlashBladeCompat.isSlashBladeItem(mainHandItem)) {
                    SlashBladeRender.renderMainhandSlashBlade(entityLivingBaseIn, geoModel, poseStack, bufferIn, packedLightIn, mainHandItem, partialTicks);
                } else {
                    TACZCompat.openFlashShellRender(entityLivingBaseIn, mainHandItem);
                    this.renderArmWithItem(geoModel, entityLivingBaseIn, mainHandItem, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, poseStack, bufferIn, packedLightIn);
                    if (renderLayersFirst && !mainHandItem.isEmpty() && !TACZCompat.isTACZItem(mainHandItem) && bufferIn instanceof IExtendedBufferSource bufferSource) {
                        bufferSource.endBatchFixedRenderType();
                    }
                    TACZCompat.stopFlashShellRender(mainHandItem);
                }
            }
            if (!geoModel.leftHandBones().isEmpty()) {
                if (SlashBladeCompat.isSlashBladeItem(offhandItem)) {
                    SlashBladeRender.renderOffhandSlashBlade(geoModel, poseStack, bufferIn, packedLightIn, offhandItem);
                } else {
                    this.renderArmWithItem(geoModel, entityLivingBaseIn, offhandItem, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, poseStack, bufferIn, packedLightIn);
                    if (renderLayersFirst && !offhandItem.isEmpty() && !TACZCompat.isTACZItem(mainHandItem) && bufferIn instanceof IExtendedBufferSource bufferSource) {
                        bufferSource.endBatchFixedRenderType();
                    }
                }
            }
            poseStack.popPose();
            // TACZ 副手枪械渲染
            TACZCompat.renderOffsetHand(offhandItem, geoModel, entityLivingBaseIn, poseStack, packedLightIn, partialTicks);
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
            RenderUtils.prepMatrixForLocator(poseStack, geoModel.leftHandBones());
        } else {
            RenderUtils.prepMatrixForLocator(poseStack, geoModel.rightHandBones());
        }
    }
}
