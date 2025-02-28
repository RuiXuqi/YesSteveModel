package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.api.IExtendedBufferSource;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.compat.slashblade.SlashBladeCompat;
import com.elfmcys.yesstevemodel.client.compat.slashblade.SlashBladeRender;
import com.elfmcys.yesstevemodel.client.compat.tacz.TACZCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
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

import java.util.List;

public class CustomPlayerItemInHandLayer extends GeoLayerRenderer<CustomPlayerEntity> {
    private final ItemInHandRenderer itemInHandRenderer;

    public CustomPlayerItemInHandLayer(ItemInHandRenderer itemInHandRenderer) {
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn, CustomPlayerEntity instance, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        LivingEntity entityLivingBaseIn = instance.getEntity();
        GeoModelState geoModel = instance.getCurrentModel();
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
                    if (renderLayersFirst && !mainHandItem.isEmpty() && bufferIn instanceof IExtendedBufferSource bufferSource) {
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
                    if (renderLayersFirst && !offhandItem.isEmpty() && bufferIn instanceof IExtendedBufferSource bufferSource) {
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
            boolean isLeftHand = arm == HumanoidArm.LEFT;

            // 渲染默认手部物品
            poseStack.pushPose();
            boolean scaleResult = translateToHand(arm, poseStack, geoModel);
            // 缩放不为 0 才会渲染
            if (!scaleResult) {
                poseStack.translate(0, -0.0625, -0.1);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                this.itemInHandRenderer.renderItem(livingEntity, itemStack, displayContext, isLeftHand, poseStack, bufferSource, light);
            }
            poseStack.popPose();

            // 渲染额外手部物品
            List<List<IBone>> extraBones = isLeftHand ? geoModel.extraLeftHandBones() : geoModel.extraRightHandBones();
            extraBones.forEach(bones -> {
                poseStack.pushPose();
                boolean extraScaleResult = RenderUtils.prepMatrixForLocator(poseStack, bones);
                // 缩放不为 0 才会渲染
                if (!extraScaleResult) {
                    poseStack.translate(0, -0.0625, -0.1);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    this.itemInHandRenderer.renderItem(livingEntity, itemStack, displayContext, isLeftHand, poseStack, bufferSource, light);
                }
                poseStack.popPose();
            });
        }
    }

    protected boolean translateToHand(HumanoidArm arm, PoseStack poseStack, GeoModelState geoModel) {
        if (arm == HumanoidArm.LEFT) {
            return RenderUtils.prepMatrixForLocator(poseStack, geoModel.leftHandBones());
        } else {
            return RenderUtils.prepMatrixForLocator(poseStack, geoModel.rightHandBones());
        }
    }
}
