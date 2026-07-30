package com.elfmcys.ysm.client.renderer;

import com.elfmcys.ysm.client.entity.CustomFirstPersonArmEntity;

public class CustomFirstPersonArmRenderer {
    private CustomFirstPersonArmEntity armEntity = null;

//    @SuppressWarnings("all")
//    public void render(LocalPlayer player, ModelRenderTarget model, PlayerAnimatableCapability cap, HumanoidArm arm,
//                       PoseStack poseStack, MultiBufferSource bufferSource,
//                       int packedLight, float partialTick) {
//        if (armEntity == null || armEntity.getEntity() != player) {
//            armEntity = new CustomFirstPersonArmEntity(player, cap);
//        }
//
//        armEntity.checkModelUpdate();
//        AnimationEvent<?> event = armEntity.updateAnimation(partialTick);
//        if (event == null) {
//            return;
//        }
//
//        AnimatedGeoModel currentModel = armEntity.getLoadedGeoModel();
//        if (currentModel == null) {
//            return;
//        }
//
//        var renderEvent = new SpecialPlayerRenderEvent(player, cap, cap.getModelId());
//        if (MinecraftForge.EVENT_BUS.post(renderEvent)) {
//            return;
//        }
//
//        var textureLocation = renderEvent.getTextureLocationOverride() == null ? cap.getTextureLocation() : renderEvent.getTextureLocationOverride();
//        var vertexConsumer = bufferSource.getBuffer(CustomTranslucentRenderType.create(textureLocation));
//        // var nativeRenderMode = arm == HumanoidArm.LEFT ? NativeRenderer.RENDER_MODE_LEFT_ARM : NativeRenderer.RENDER_MODE_RIGHT_ARM;
//
//        poseStack.pushPose();
//        if (arm == HumanoidArm.LEFT) {
//            poseStack.translate(0.25, 1.8, 0);
//        } else {
//            poseStack.translate(-0.25, 1.8, 0);
//        }
//        poseStack.scale(-1, -1, 1);
//        // TODO
////        NativeRenderer.renderModel(vertexConsumer, poseStack.last(), currentModel.getModelData(), currentModel.inputState(),
////                currentModel.outputState(), nativeRenderMode, packedLight, OverlayTexture.NO_OVERLAY,
////                1, 1, 1, 1);
//        poseStack.popPose();
//    }
}
