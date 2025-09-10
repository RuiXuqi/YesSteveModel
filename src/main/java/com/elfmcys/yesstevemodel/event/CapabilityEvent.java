package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.*;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.ServerInfo;
import com.elfmcys.yesstevemodel.network.message.SyncAuthModels;
import com.elfmcys.yesstevemodel.network.message.SyncProjectileModelInfo;
import com.elfmcys.yesstevemodel.network.message.SyncStarModels;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod.EventBusSubscriber
@SuppressWarnings("removal")
public final class CapabilityEvent {
    private static final ResourceLocation MODEL_INFO_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "model_id");
    private static final ResourceLocation PROJECTILE_MODEL_INFO_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "projectile_model_id");
    private static final ResourceLocation AUTH_MODELS_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "own_models");
    private static final ResourceLocation STAR_MODELS_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "star_models");
    private static final ResourceLocation ANIMATABLE_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "animatable");
    private static final ResourceLocation PROJECTILE_ANIMATABLE_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "projectile_animatable");

    @SubscribeEvent
    @SuppressWarnings("resource")
    public static void onAttachCapabilityEvent(AttachCapabilitiesEvent<Entity> event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        Entity entity = event.getObject();
        if (entity instanceof Player player) {
            if (!entity.level().isClientSide() && !player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).isPresent() && !event.getCapabilities().containsKey(MODEL_INFO_CAP)) {
                event.addCapability(MODEL_INFO_CAP, new ModelInfoCapabilityProvider());
            }
            if (!player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).isPresent() && !event.getCapabilities().containsKey(AUTH_MODELS_CAP)) {
                event.addCapability(AUTH_MODELS_CAP, new AuthModelsCapabilityProvider());
            }
            if (!player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP).isPresent() && !event.getCapabilities().containsKey(STAR_MODELS_CAP)) {
                event.addCapability(STAR_MODELS_CAP, new StarModelsCapabilityProvider());
            }
        } else if (entity instanceof Projectile) {
            if (!entity.level().isClientSide() && !entity.getCapability(ProjectileModelInfoCapabilityProvider.CAP).isPresent() && !event.getCapabilities().containsKey(PROJECTILE_MODEL_INFO_CAP)) {
                event.addCapability(PROJECTILE_MODEL_INFO_CAP, new ProjectileModelInfoCapabilityProvider());
            }
        }
        if (FMLEnvironment.dist == Dist.CLIENT && entity.level().isClientSide()) {
            if (entity instanceof AbstractClientPlayer clientPlayer && !clientPlayer.getCapability(PlayerAnimatableCapabilityProvider.CAP).isPresent() && !event.getCapabilities().containsKey(ANIMATABLE_CAP)) {
                event.addCapability(ANIMATABLE_CAP, new PlayerAnimatableCapabilityProvider(clientPlayer));
            } else if (entity instanceof Projectile projectile && !entity.getCapability(ProjectileAnimatableCapabilityProvider.CAP).isPresent() && !event.getCapabilities().containsKey(PROJECTILE_ANIMATABLE_CAP)) {
                event.addCapability(PROJECTILE_ANIMATABLE_CAP, new ProjectileAnimatableCapabilityProvider(projectile));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
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
        if (!YesSteveModel.isAvailable()) {
            return;
        }
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
        } else if (event.getTarget() instanceof Projectile projectile) {
            projectile.getCapability(ProjectileModelInfoCapabilityProvider.CAP).ifPresent(cap -> {
                if (cap.isInitialized()) {
                    NetworkHandler.sendToClientPlayer(new SyncProjectileModelInfo(projectile.getId(), cap), event.getEntity());
                }
            });
        }
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
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
        if (!YesSteveModel.isAvailable()) {
            return;
        }
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
                    cap.buildPacketForDispatch(player).ifPresent(packet -> {
                        cap.clearDirty();
                        NetworkHandler.broadcastToVisiblePlayersAndSelf(packet, player);
                    });
                    cap.getPropertiesTracker().tick(player, cap.isDirty());
                } else {
                    cap.getPropertiesTracker().tick(player, true);
                }
            });
        }
    }

    public static void onProjectileSetOwner(Projectile projectile, ServerPlayer owner) {
        owner.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(ownerCap -> {
            if (!NetworkHandler.isPlayerChannelPresent(owner) && !ownerCap.isMandatory()) {
                return;
            }
            projectile.getCapability(ProjectileModelInfoCapabilityProvider.CAP).ifPresent(cap -> {
                cap.init(ownerCap.getModelId());
                NetworkHandler.broadcastToVisiblePlayers(new SyncProjectileModelInfo(projectile.getId(), cap), projectile);
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
