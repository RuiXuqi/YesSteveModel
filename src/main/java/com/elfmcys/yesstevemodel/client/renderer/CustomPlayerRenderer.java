package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapability;
import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.gui.GuiModelInstance;
import com.elfmcys.yesstevemodel.client.instance.CustomPlayerInstance;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerElytraLayer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerItemInHandLayer;
import com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoInstance;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;

public class CustomPlayerRenderer extends GeoReplacedEntityRenderer<AbstractClientPlayer, CustomPlayerInstance> {

    @SuppressWarnings("all")
    public CustomPlayerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        addLayer(new CustomPlayerItemInHandLayer(ctx.getItemInHandRenderer()));
        addLayer(new CustomPlayerElytraLayer(ctx));
    }

    @Override
    @SuppressWarnings("all")
    public void render(AbstractClientPlayer player, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        PlayerGeoCapability cap = player.getCapability(PlayerGeoCapabilityProvider.CAP).orElse(null);
        if (cap == null) {
            return;
        }

        if (MinecraftForge.EVENT_BUS.post(new SpecialPlayerRenderEvent(player, cap.getAnimatable(), cap.getModelId()))) {
            return;
        }

        renderGeoInstance(cap, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    public void renderModelInGui(GuiModelInstance instance, float entityYaw, float partialTick, PoseStack poseStack,
                                 MultiBufferSource bufferSource, int packedLight) {
        renderGeoInstance(instance, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public RenderType getRenderType(CustomPlayerInstance animatable, float partialTick, PoseStack poseStack, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, int packedLight, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public boolean shouldShowName(AbstractClientPlayer entity) {
        double distance = this.entityRenderDispatcher.distanceToSqr(entity);
        float renderDistance = entity.isDiscrete() ? 32.0F : 64.0F;
        if (distance >= (double) (renderDistance * renderDistance)) {
            return false;
        } else {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player == null) {
                return false;
            }
            boolean invisible = !entity.isInvisibleTo(player);
            if (entity != player) {
                Team team1 = entity.getTeam();
                Team team2 = player.getTeam();
                if (team1 != null) {
                    Team.Visibility team$visibility = team1.getNameTagVisibility();
                    return switch (team$visibility) {
                        case ALWAYS -> invisible;
                        case NEVER -> false;
                        case HIDE_FOR_OTHER_TEAMS ->
                                team2 == null ? invisible : team1.isAlliedTo(team2) && (team1.canSeeFriendlyInvisibles() || invisible);
                        case HIDE_FOR_OWN_TEAM -> team2 == null ? invisible : !team1.isAlliedTo(team2) && invisible;
                    };
                }
            }
            return Minecraft.renderNames() && entity != minecraft.getCameraEntity() && invisible && !entity.isVehicle();
        }
    }

    @Override
    @Deprecated
    public ResourceLocation getTextureLocation(AbstractClientPlayer pEntity) {
        return pEntity.getCapability(PlayerGeoCapabilityProvider.CAP).map(GeoInstance::getTextureLocation).orElse(MissingTextureAtlasSprite.getLocation());
    }

    @Override
    @SuppressWarnings("all")
    protected void renderNameTag(AbstractClientPlayer player, Component displayName, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        double distance = this.entityRenderDispatcher.distanceToSqr(player);
        poseStack.pushPose();
        if (distance < 100) {
            Scoreboard scoreboard = player.getScoreboard();
            Objective objective = scoreboard.getDisplayObjective(2);
            if (objective != null) {
                Score score = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);
                super.renderNameTag(player, (Component.literal(Integer.toString(score.getScore()))).append(" ").append(objective.getDisplayName()), poseStack, buffer, packedLight);
                poseStack.translate(0, 9.0 * 1.15 * 0.025, 0);
            }
        }
        super.renderNameTag(player, displayName, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
