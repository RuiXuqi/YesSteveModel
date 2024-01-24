package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.*;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SyncAuthModels;
import com.elfmcys.yesstevemodel.network.message.SyncModelInfo;
import com.elfmcys.yesstevemodel.network.message.SyncStarModels;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.LogicalSide;

public final class CapabilityEvent {
    private static final ResourceLocation MODEL_INFO_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "model_id");
    private static final ResourceLocation AUTH_MODELS_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "own_models");
    private static final ResourceLocation STAR_MODELS_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "star_models");
    private static final ResourceLocation GEO_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "geo");

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
        }
    }

    public static void onAttachClientCapabilityEvent(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof AbstractClientPlayer player) {
            if (!player.getCapability(PlayerGeoCapabilityProvider.CAP).isPresent() && !event.getCapabilities().containsKey(GEO_CAP)) {
                event.addCapability(GEO_CAP, new PlayerGeoCapabilityProvider(player));
            }
        }
    }

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

    public static void onTrackingPlayer(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player trackPlayer) {
            final Player player = event.getEntity();
            getModelInfoCap(trackPlayer).ifPresent(cap -> {
                SyncModelInfo syncMsg = new SyncModelInfo(trackPlayer.getId(), cap);
                NetworkHandler.sendToClientPlayer(syncMsg, player);
            });
        }
    }

    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Player player) {
            getModelInfoCap(player).ifPresent(modelInfoCap -> {
                if (player instanceof ServerPlayer serverPlayer) {
                    NetworkHandler.sendToClientPlayer(new SyncModelInfo(serverPlayer.getId(), modelInfoCap), serverPlayer);
                } else {
                    modelInfoCap.markDirty();
                }
            });

            if (player instanceof ServerPlayer) {
                final ServerPlayer serverPlayer = (ServerPlayer) player;

                getAuthModelsCap(player).ifPresent(authModelsCap -> {
                    NetworkHandler.sendToClientPlayer(new SyncAuthModels(authModelsCap.getAuthModels()), serverPlayer);
                });

                getStarModelsCap(player).ifPresent(starModelCap -> {
                    NetworkHandler.sendToClientPlayer(new SyncStarModels(starModelCap.getStarModels()), serverPlayer);
                });
            }
        }
    }

    /**
     * 同步客户端服务端数据
     */
    public static void onPlayerTickEvent(TickEvent.PlayerTickEvent event) {
        final Player player = event.player;
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END) {
            getModelInfoCap(player).ifPresent(cap -> {
                if (cap.isDirty()) {
                    SyncModelInfo syncMsg = new SyncModelInfo(player.getId(), cap);
                    if (player.getServer() == null) {
                        return;
                    }
                    NetworkHandler.broadcastToVisiblePlayersAndSelf(syncMsg, player);
                    cap.setDirty(false);
                }
            });
        }
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
