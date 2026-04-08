package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.render;

import com.elfmcys.yesstevemodel.capability.VehicleAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability.YsmMaidCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.CustomYsmMaidEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoLayerRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntityRenderer;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class CustomYsmMaidRenderer extends GeoReplacedEntityRenderer<EntityMaid, CustomYsmMaidEntity> implements IGeoEntityRenderer<EntityMaid> {
    /**
     * 屎山代码 +1，因为船和矿车有特殊偏移，需要修正
     */
    private static final Set<EntityType> OFFSET_FIX_ENTITIES = Set.of(EntityType.MINECART, EntityType.BOAT, InitEntities.BROOM.get());
    protected final List<GeoLayerRenderer<EntityMaid, CustomYsmMaidRenderer>> maidLayers = new ObjectArrayList<>();

    public CustomYsmMaidRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public CustomYsmMaidEntity getAnimatableEntity(EntityMaid maid) {
        return maid.getCapability(YsmMaidCapabilityProvider.CAP).map(e -> e).orElseGet(() -> new CustomYsmMaidEntity(maid, true));
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
            renderAnimatableEntity(customGeoMaidEntity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        });
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(EntityMaid maid) {
        return maid.getCapability(YsmMaidCapabilityProvider.CAP).map(CustomYsmMaidEntity::getTextureLocation).orElse(MissingTextureAtlasSprite.getLocation());
    }

    @Override
    protected void renderLayer(CustomYsmMaidEntity animatableEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AnimationEvent<?> event, EntityModelData data) {
        for (GeoLayerRenderer<EntityMaid, CustomYsmMaidRenderer> maidLayer : maidLayers) {
            maidLayer.render(poseStack, bufferSource, packedLight, animatableEntity.getEntity(), event.getLimbSwing(), event.getLimbSwingAmount(), partialTick,
                    data.lerpedAge, data.netHeadYaw, data.headPitch);
        }
    }

    @Override
    protected void setupRotations(EntityMaid maid, PoseStack poseStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
        super.setupRotations(maid, poseStack, pAgeInTicks, pRotationYaw, pPartialTicks);
        // 如果女仆待命状态，需要下移三格
        if (maid.isMaidInSittingPose()) {
            poseStack.translate(0, -0.5, 0);
            return;
        }

        Entity vehicle = maid.getVehicle();

        // 抱起女仆需要额外的偏移
        if (vehicle instanceof Player) {
            poseStack.translate(-0.05, 0.19, 0.24);
            return;
        }

        //特定的没有 cap 的实体应该下移三格，避免悬空
        if (vehicle != null && OFFSET_FIX_ENTITIES.contains(vehicle.getType())
            && !vehicle.getCapability(VehicleAnimatableCapabilityProvider.CAP).isPresent()) {
            poseStack.translate(0, -0.5, 0);
        }
    }
}
