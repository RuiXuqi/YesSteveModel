package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.render;

import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability.YsmMaidCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.CustomYsmMaidEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoLayerRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CustomYsmMaidRenderer extends GeoReplacedEntityRenderer<EntityMaid, CustomYsmMaidEntity> implements IGeoEntityRenderer<EntityMaid> {
    protected final List<GeoLayerRenderer<EntityMaid, CustomYsmMaidRenderer>> maidLayers = new ObjectArrayList<>();

    public CustomYsmMaidRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public CustomYsmMaidEntity getAnimatableEntity(EntityMaid maid) {
        return maid.getCapability(YsmMaidCapabilityProvider.CAP).map(e -> e).orElse(new CustomYsmMaidEntity(maid, true));
    }

    @Override
    public IGeoEntity getGeoEntity(EntityMaid maid) {
        return this.getAnimatableEntity(maid);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addGeoLayerRenderer(GeoLayerRenderer<?, ?> layerRenderer) {
        this.maidLayers.add((GeoLayerRenderer<EntityMaid, CustomYsmMaidRenderer>) layerRenderer);
    }

    @Override
    public void geoRender(EntityMaid entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        entity.getCapability(YsmMaidCapabilityProvider.CAP).ifPresent(customGeoMaidEntity -> {
            renderGeoInstance(customGeoMaidEntity, null, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        });
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(EntityMaid maid) {
        return maid.getCapability(YsmMaidCapabilityProvider.CAP).map(CustomYsmMaidEntity::getTextureLocation).orElse(MissingTextureAtlasSprite.getLocation());
    }

    @Override
    protected void renderLayer(CustomYsmMaidEntity instance, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AnimationEvent<?> event, EntityModelData data) {
        for (GeoLayerRenderer<EntityMaid, CustomYsmMaidRenderer> maidLayer : maidLayers) {
            maidLayer.render(poseStack, bufferSource, packedLight, instance.getEntity(), event.getLimbSwing(), event.getLimbSwingAmount(), partialTick,
                    data.lerpedAge, data.netHeadYaw, data.headPitch);
        }
    }

    @Override
    protected void setupRotations(EntityMaid maid, PoseStack poseStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
        super.setupRotations(maid, poseStack, pAgeInTicks, pRotationYaw, pPartialTicks);
        // 如果女仆待命状态，需要下移三格
        if (maid.isMaidInSittingPose()) {
            poseStack.translate(0, -0.5, 0);
        }
        // 女仆骑乘也是，但是要排除抱起女仆
        if (maid.getVehicle() != null && !(maid.getVehicle() instanceof Player)) {
            poseStack.translate(0, -0.5, 0);
        }
    }
}
