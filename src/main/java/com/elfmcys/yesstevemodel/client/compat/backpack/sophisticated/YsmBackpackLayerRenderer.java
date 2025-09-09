package com.elfmcys.yesstevemodel.client.compat.backpack.sophisticated;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.client.render.BackpackModelManager;
import net.p3pp3rf1y.sophisticatedbackpacks.client.render.IBackpackModel;

import static net.p3pp3rf1y.sophisticatedbackpacks.client.render.BackpackLayerRenderer.renderBackpack;

public class YsmBackpackLayerRenderer extends GeoLayerRenderer<CustomPlayerEntity> {
    private final EntityModel<Player> model;

    YsmBackpackLayerRenderer() {
        BackpackModelManager.initModels();
        this.model = getEmptyModel();
    }

    /**
     * 空 EntityModel，仅用于渲染背包时的占位符
     */
    @SuppressWarnings("all")
    private static EntityModel<Player> getEmptyModel() {
        return new EntityModel<>() {
            @Override
            public void setupAnim(Player entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            }

            @Override
            public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
            }
        };
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn, CustomPlayerEntity animatableEntity,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        GeoModelState geoModel = animatableEntity.getLoadedGeoModel();
        if (geoModel == null || geoModel.backpackBones().isEmpty()) {
            return;
        }
        Player player = animatableEntity.getEntity();
        ItemStack backpack = SophisticatedCompat.getBackpackItemStack(player);
        // 渲染
        if (backpack != null) {
            poseStack.pushPose();
            IBackpackModel model = BackpackModelManager.getBackpackModel(backpack.getItem());
            translateToBackpack(poseStack, geoModel);
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.translate(0, -0.1, 0);
            renderBackpack(this.model, player, poseStack, bufferIn, packedLightIn, backpack, false, model);
            poseStack.popPose();
        }
    }

    protected void translateToBackpack(PoseStack poseStack, GeoModelState geoModel) {
        RenderUtils.prepMatrixForLocator(poseStack, geoModel.backpackBones());
    }
}
