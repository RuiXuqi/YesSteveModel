package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapability;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.entity.CustomFirstPersonArmEntity;
import com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.geo.CustomTranslucentRenderType;
import com.elfmcys.yesstevemodel.geckolib3.geo.NativeRenderer;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.common.MinecraftForge;

import static com.elfmcys.yesstevemodel.geckolib3.geo.NativeRenderer.isAsyncScope;

public class CustomFirstPersonArmRenderer {
    private CustomFirstPersonArmEntity armEntity = null;

    @SuppressWarnings("all")
    public void render(LocalPlayer player, ClientModel model, PlayerAnimatableCapability cap, HumanoidArm arm,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, float partialTick) {
        if (armEntity == null || armEntity.getEntity() == null) {
            armEntity = new CustomFirstPersonArmEntity(player, true, true);
        }
        String modelId = cap.getModelId();
        String textureName = cap.getTextureName();
        if (!armEntity.getModelId().equals(modelId) || !armEntity.getTextureName().equals(textureName)) {
            armEntity.setModelAndTexture(cap.getModelId(), cap.getTextureName());
        }
        var renderEvent = new SpecialPlayerRenderEvent(player, armEntity, armEntity.getModelId());
        if (MinecraftForge.EVENT_BUS.post(renderEvent)) {
            return;
        }
        AnimationEvent<?> event = isAsyncScope() ? armEntity.waitOrUpdate(partialTick) : armEntity.syncUpdate(partialTick);
        if (event == null) {
            return;
        }

        ResourceLocation textureLocation = renderEvent.getTextureLocationOverride() != null ? renderEvent.getTextureLocationOverride() : cap.getTextureLocation();
        int textureIndex = renderEvent.getTextureLocationOverride() == null ? cap.getTextureIndex() : 0;
        var vertexConsumer = bufferSource.getBuffer(CustomTranslucentRenderType.create(textureLocation));
        int nativeRenderMode = arm == HumanoidArm.LEFT ? NativeRenderer.RENDER_MODE_LEFT_ARM : NativeRenderer.RENDER_MODE_RIGHT_ARM;
        GeoModelState currentModel = armEntity.getCurrentModel();

        poseStack.pushPose();
        if (arm == HumanoidArm.LEFT) {
            poseStack.translate(0.25, 1.8, 0);
        } else {
            poseStack.translate(-0.25, 1.8, 0);
        }
        poseStack.scale(-1, -1, 1);
        NativeRenderer.renderModel(vertexConsumer, poseStack.last(), currentModel.model(), currentModel.inputState(),
                currentModel.outputState(), textureIndex, nativeRenderMode, packedLight, OverlayTexture.NO_OVERLAY,
                1, 1, 1, 1);
        poseStack.popPose();
    }
}
