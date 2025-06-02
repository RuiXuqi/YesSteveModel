package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Parrot;

public class CustomParrotOnShoulderLayer extends GeoLayerRenderer<CustomPlayerEntity> {
    private static final String ID = "id";
    private static final String VARIANT = "Variant";
    private final ParrotModel model;

    public CustomParrotOnShoulderLayer(EntityRendererProvider.Context context) {
        this.model = new ParrotModel(context.bakeLayer(ModelLayers.PARROT));
    }

    @Override
    public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, CustomPlayerEntity animatableEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        AbstractClientPlayer player = animatableEntity.getEntity();
        GeoModelState geoModel = animatableEntity.getCurrentModel();
        if (geoModel == null) {
            return;
        }
        if (!geoModel.leftShoulderBones().isEmpty()) {
            this.render(matrixStackIn, bufferIn, geoModel, packedLightIn, player, limbSwing, limbSwingAmount, netHeadYaw, headPitch, true);
        }
        if (!geoModel.rightShoulderBones().isEmpty()) {
            this.render(matrixStackIn, bufferIn, geoModel, packedLightIn, player, limbSwing, limbSwingAmount, netHeadYaw, headPitch, false);
        }
    }

    private void render(PoseStack poseStack, MultiBufferSource buffer, GeoModelState geoModel, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch, boolean leftShoulder) {
        CompoundTag shoulderTag = leftShoulder ? player.getShoulderEntityLeft() : player.getShoulderEntityRight();
        EntityType.byString(shoulderTag.getString(ID)).filter(type -> type == EntityType.PARROT).ifPresent(type -> {
            poseStack.pushPose();
            translateToShoulder(poseStack, geoModel, leftShoulder);
            poseStack.translate(0, 1.5, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            Parrot.Variant variant = Parrot.Variant.byId(shoulderTag.getInt(VARIANT));
            VertexConsumer bufferBuffer = buffer.getBuffer(this.model.renderType(ParrotRenderer.getVariantTexture(variant)));
            this.model.renderOnShoulder(poseStack, bufferBuffer, packedLight, OverlayTexture.NO_OVERLAY, limbSwing, limbSwingAmount, netHeadYaw, headPitch, player.tickCount);
            poseStack.popPose();
        });
    }

    protected void translateToShoulder(PoseStack poseStack, GeoModelState geoModel, boolean leftShoulder) {
        if (leftShoulder) {
            RenderUtils.prepMatrixForLocator(poseStack, geoModel.leftShoulderBones());
        } else {
            RenderUtils.prepMatrixForLocator(poseStack, geoModel.rightShoulderBones());
        }
    }
}
