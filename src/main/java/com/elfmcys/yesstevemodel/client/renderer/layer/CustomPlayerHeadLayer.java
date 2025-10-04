package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.client.compat.simplehat.SimpleHatsCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import static net.minecraft.world.entity.EquipmentSlot.HEAD;

public class CustomPlayerHeadLayer extends GeoLayerRenderer<CustomPlayerEntity> {
    private final ItemInHandRenderer handRenderer;

    public CustomPlayerHeadLayer(EntityRendererProvider.Context context) {
        this.handRenderer = context.getItemInHandRenderer();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn,
                       CustomPlayerEntity animatableEntity, float pLimbSwing, float pLimbSwingAmount,
                       float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        var player = animatableEntity.getEntity();
        GeoModelState geoModel = animatableEntity.getLoadedGeoModel();
        if (geoModel != null && !geoModel.headBones().isEmpty()) {
            ItemStack head = player.getItemBySlot(HEAD);
            if (!head.isEmpty() && !isArmorHead(head)) {
                renderHeadItem(poseStack, bufferIn, packedLightIn, geoModel, player, head);
            }
            ItemStack curiosHead = SimpleHatsCompat.getCuriosHead(player);
            if (curiosHead != null && !curiosHead.isEmpty()) {
                renderHeadItem(poseStack, bufferIn, packedLightIn, geoModel, player, curiosHead);
            }
        }
    }

    private boolean isArmorHead(ItemStack itemStack) {
        return itemStack.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == HEAD;
    }

    private void renderHeadItem(PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn, GeoModelState geoModel, Player player, ItemStack head) {
        poseStack.pushPose();
        RenderUtils.prepMatrixForLocator(poseStack, geoModel.headBones());
        poseStack.scale(0.625F, 0.625F, 0.625F);
        poseStack.translate(0.0F, 0.25F, 0.0F);
        this.handRenderer.renderItem(player, head, ItemDisplayContext.HEAD,
                false, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }
}
