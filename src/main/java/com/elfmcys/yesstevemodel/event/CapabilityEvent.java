package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.*;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public final class CapabilityEvent {
    private static final ResourceLocation MODEL_INFO_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "model_id");
    private static final ResourceLocation ARROW_MODEL_INFO_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "arrow_model_id");
    private static final ResourceLocation AUTH_MODELS_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "own_models");
    private static final ResourceLocation STAR_MODELS_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "star_models");
    private static final ResourceLocation ANIMATABLE_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "animatable");
    private static final ResourceLocation PROJECTILE_ANIMATABLE_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "projectile_animatable");

    @SubscribeEvent
    public static void onAttachCapabilityEvent(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof Player player) {
            if (entity instanceof ServerPlayer && !player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).isPresent() && !event.getCapabilities().containsKey(MODEL_INFO_CAP)) {
                event.addCapability(MODEL_INFO_CAP, new ModelInfoCapabilityProvider());
            }
            if (!player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).isPresent() && !event.getCapabilities().containsKey(AUTH_MODELS_CAP)) {
                event.addCapability(AUTH_MODELS_CAP, new AuthModelsCapabilityProvider());
            }
            if (!player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP).isPresent() && !event.getCapabilities().containsKey(STAR_MODELS_CAP)) {
                event.addCapability(STAR_MODELS_CAP, new StarModelsCapabilityProvider());
            }
            if (entity.level().isClientSide() && event.getObject() instanceof AbstractClientPlayer clientPlayer && !clientPlayer.getCapability(PlayerAnimatableCapabilityProvider.CAP).isPresent() && !event.getCapabilities().containsKey(ANIMATABLE_CAP)) {
                event.addCapability(ANIMATABLE_CAP, new PlayerAnimatableCapabilityProvider(clientPlayer));
            }
        } else if (entity instanceof AbstractArrow) {
            if (entity.level().isClientSide() && !entity.getCapability(ArrowGeoCapabilityProvider.CAP).isPresent() && !event.getCapabilities().containsKey(ARROW_MODEL_INFO_CAP)) {
                event.addCapability(ARROW_MODEL_INFO_CAP, new ArrowGeoCapabilityProvider((AbstractArrow) entity));
            } else if (!entity.level().isClientSide() && !entity.getCapability(ArrowModelInfoCapabilityProvider.CAP).isPresent() && !event.getCapabilities().containsKey(PROJECTILE_ANIMATABLE_CAP)) {
                event.addCapability(PROJECTILE_ANIMATABLE_CAP, new ArrowModelInfoCapabilityProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        LazyOptional<ModelInfoCapability> oldModelInfoCap = getModelInfoCap(event.getOriginal());
        LazyOptional<AuthModelsCapability> oldAuthModelsCap = getAuthModelsCap(event.getOriginal());
        LazyOptional<StarModelsCapability> oldStarModelsCap = getStarModelsCap(event.getOriginal());
        event.getOriginal().invalidateCaps();

        LazyOptional<ModelInfoCapability> newModelInfoCap = getModelInfoCap(event.getEntity());
        LazyOptional<AuthModelsCapability> newAuthModelsCap = getAuthModelsCap(event.getEntity());
        LazyOptional<StarModelsCapability> newStarModelsCap = getStarModelsCap(event.getEntity());

        newModelInfoCap.ifPresent((newModelInfo) -> oldModelInfoCap.ifPresent(newModelInfo::copyFrom));
        newAuthModelsCap.ifPresent((newAuthModels) -> oldAuthModelsCap.ifPresent(newAuthModels::copyFrom));
        newStarModelsCap.ifPresent((newStarModels) -> oldStarModelsCap.ifPresent(newStarModels::copyFrom));
    }

    @SubscribeEvent
    public static void onTrackingPlayer(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer trackPlayer) {
            final Player player = event.getEntity();
            getModelInfoCap(trackPlayer).ifPresent(cap -> {
                if (!NetworkHandler.isPlayerChannelPresent(trackPlayer) && !cap.isMandatory()) {
                    return;
                }
                cap.buildPacketForDispatch(trackPlayer).ifPresentOrElse(packet -> {
                    NetworkHandler.sendToClientPlayer(packet, player);
                }, cap::markDirty);
            });
        } else if (event.getTarget() instanceof AbstractArrow arrow) {
            arrow.getCapability(ArrowModelInfoCapabilityProvider.CAP).ifPresent(cap -> {
                if (cap.isInitialized()) {
                    NetworkHandler.sendToClientPlayer(new SyncArrowModelInfo(arrow.getId(), cap), event.getEntity());
                }
            });
        }
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            getModelInfoCap(serverPlayer).ifPresent(modelInfoCap -> {
                if (!NetworkHandler.isPlayerChannelPresent(serverPlayer) && !modelInfoCap.isMandatory()) {
                    modelInfoCap.markDirty();
                    return;
                }
                modelInfoCap.stopAnimation();
                modelInfoCap.buildPacketForDispatch(serverPlayer).ifPresentOrElse(packet -> {
                    NetworkHandler.sendToClientPlayer(packet, serverPlayer);
                }, modelInfoCap::markDirty);
            });

            getAuthModelsCap(serverPlayer).ifPresent(authModelsCap -> {
                NetworkHandler.sendToClientPlayer(new SyncAuthModels(authModelsCap.getAuthModels()), serverPlayer);
            });

            getStarModelsCap(serverPlayer).ifPresent(starModelCap -> {
                NetworkHandler.sendToClientPlayer(new SyncStarModels(starModelCap.getStarModels()), serverPlayer);
            });
        }
    }

    /**
     * 同步客户端服务端数据
     */
    @SubscribeEvent
    public static void onPlayerTickEvent(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && event.player instanceof ServerPlayer player) {
            getModelInfoCap(player).ifPresent(cap -> {
                if (!NetworkHandler.isPlayerChannelPresent(player) && !cap.isMandatory()) {
                    if (player.tickCount == 200 || player.tickCount == 600 || player.tickCount == 1800) {
                        NetworkHandler.sendToClientPlayer(new ServerInfo(), player);
                    }
                    return;
                }
                if (cap.isDirty()) {
                    if (player.getServer() != null) {
                        cap.buildPacketForDispatch(player).ifPresent(packet -> {
                            cap.clearDirty();
                            NetworkHandler.broadcastToVisiblePlayersAndSelf(packet, player);
                        });
                    }
                }
            });
        }
    }

    public static void onArrowSetOwner(AbstractArrow arrow, ServerPlayer owner) {
        owner.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(ownerCap -> {
            if (!NetworkHandler.isPlayerChannelPresent(owner) && !ownerCap.isMandatory()) {
                return;
            }
            arrow.getCapability(ArrowModelInfoCapabilityProvider.CAP).ifPresent(arrowCap -> {
                arrowCap.init(ownerCap.getModelId());
                NetworkHandler.broadcastToVisiblePlayers(new SyncArrowModelInfo(arrow.getId(), arrowCap), arrow);
            });
        });
    }

    private static LazyOptional<ModelInfoCapability> getModelInfoCap(Player player) {
        return player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP);
    }

    private static LazyOptional<AuthModelsCapability> getAuthModelsCap(Player player) {
        return player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP);
    }

    private static LazyOptional<StarModelsCapability> getStarModelsCap(Player player) {
        return player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP);
    }
}
