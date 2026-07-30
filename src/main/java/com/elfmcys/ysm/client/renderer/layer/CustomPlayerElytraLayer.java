package com.elfmcys.ysm.client.renderer.layer;

import com.elfmcys.ysm.client.entity.CustomPlayerEntity;
import com.elfmcys.ysm.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.ysm.geckolib3.geo.GeoRenderData;
import com.elfmcys.ysm.geckolib3.model.AnimatedGeoModel;
import com.elfmcys.ysm.util.EquipmentUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class CustomPlayerElytraLayer extends GeoLayerRenderer<CustomPlayerEntity> {
    private static final ResourceLocation WINGS_LOCATION = new ResourceLocation("textures/entity/elytra.png");
    private final ElytraModel<LivingEntity> elytraModel;

    public CustomPlayerElytraLayer(EntityRendererProvider.Context context) {
        elytraModel = new ElytraModel<>(context.getModelSet().bakeLayer(ModelLayers.ELYTRA));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, CustomPlayerEntity animatable, GeoRenderData renderData, int packedLight, int overlay) {
        var player = animatable.getEntity();
        ItemStack stack = EquipmentUtil.getEquippedElytraItem(player);
        AnimatedGeoModel geoModel = animatable.getLoadedGeoModel();
        // TODO
//        if (!stack.isEmpty() && geoModel != null && !geoModel.elytraBones().isEmpty() && player instanceof AbstractClientPlayer clientPlayer) {
//            ResourceLocation texture;
//            if (clientPlayer.isElytraLoaded() && clientPlayer.getElytraTextureLocation() != null) {
//                texture = clientPlayer.getElytraTextureLocation();
//            } else if (clientPlayer.isCapeLoaded() && clientPlayer.getCloakTextureLocation() != null && player.isModelPartShown(PlayerModelPart.CAPE)) {
//                texture = clientPlayer.getCloakTextureLocation();
//            } else {
//                texture = WINGS_LOCATION;
//            }
//            poseStack.pushPose();
//            translateToElytra(poseStack, geoModel);
//            poseStack.translate(0, 1.5, 0);
//            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
//            poseStack.scale(2.0f, 2.0f, 2.0f);
//            this.elytraModel.setupAnim(player, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
//            VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(bufferIn, RenderType.armorCutoutNoCull(texture), false, stack.hasFoil());
//            this.elytraModel.renderToBuffer(poseStack, vertexConsumer, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
//            poseStack.popPose();
//        }
    }

//    protected void translateToElytra(PoseStack poseStack, AnimatedGeoModel geoModel) {
//        RenderUtils.prepMatrixForLocator(poseStack, geoModel.elytraBones());
//    }
}
