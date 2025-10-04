package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapability;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compat.swarfare.SWarfareCompat;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.TlmClientCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.gui.CustomGuiPlayerEntity;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomParrotOnShoulderLayer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerElytraLayer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerHeadLayer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerItemInHandLayer;
import com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;

public class CustomPlayerRenderer extends GeoReplacedEntityRenderer<Player, CustomPlayerEntity> {
    private ResourceLocation textureOverride;

    @SuppressWarnings("all")
    public CustomPlayerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        addLayer(new CustomPlayerItemInHandLayer(ctx.getItemInHandRenderer()));
        addLayer(new CustomPlayerElytraLayer(ctx));
        addLayer(new CustomParrotOnShoulderLayer(ctx));
        addLayer(new CustomPlayerHeadLayer(ctx));
    }

    @Override
    @SuppressWarnings("all")
    public void render(Player player, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // 检查卓越前线的隐藏功能
        if (SWarfareCompat.shouldHidePlayerRender(player)) {
            return;
        }

        PlayerAnimatableCapability cap = player.getCapability(PlayerAnimatableCapabilityProvider.CAP).orElse(null);
        if (cap == null) {
            return;
        }

        cap.checkModelUpdate();
        var event = new SpecialPlayerRenderEvent(player, cap, cap.getModelId());
        textureOverride = event.getTextureLocationOverride();
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return;
        }

        renderAnimatableEntity(cap, event.getTextureLocationOverride(), entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldShowName(Player entity) {
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
    @NotNull
    public ResourceLocation getTextureLocation(Player pEntity) {
        return textureOverride == null ? pEntity.getCapability(PlayerAnimatableCapabilityProvider.CAP).map(CustomPlayerEntity::getTextureLocation).orElse(MissingTextureAtlasSprite.getLocation()) : textureOverride;
    }

    @Override
    @SuppressWarnings("all")
    protected void renderNameTag(Player player, Component displayName, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (CustomGuiPlayerEntity.isFakePlayer(player)) {
            return;
        }
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

    @Override
    protected void setupRotations(Player player, PoseStack poseStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
        super.setupRotations(player, poseStack, pAgeInTicks, pRotationYaw, pPartialTicks);
        // 如果是坐在女仆的实体上，则需要偏移回去（哎，屎山代码+1006）
        Entity vehicle = player.getVehicle();
        if (TlmClientCompat.isChair(vehicle) || TlmClientCompat.isSit(vehicle)) {
            poseStack.translate(0, 0.5, 0);
        }
    }
}
