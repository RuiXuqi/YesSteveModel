package com.elfmcys.yesstevemodel.model;

import com.elfmcys.yesstevemodel.capability.AuthModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SyncAuthModels;
import com.elfmcys.yesstevemodel.network.message.SyncDataToClient;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.elfmcys.yesstevemodel.util.ThreadTools;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

// Native Access
public final class ServerModelManager {
    /**
     * 自定义模型所放置的文件夹
     * 为了统一，现在这些目录在 native 层定义
     */
    // Native Access: jni 初始化时写入
    public static Path CUSTOM;
    // Native Access: jni 初始化时写入
    public static Path AUTH;
    /**
     * 模型名称 -> 模型额外信息缓存
     * 可以方便的通过此缓存，来判断客户端发来的模型名称在不在服务端
     * 还可以获取其他服务端模型信息
     */
    private static Map<String, ServerModel> MODELS = Maps.newHashMap();
    /**
     * 放置授权模型名称
     */
    private static Set<String> AUTH_MODELS = Sets.newHashSet();

    public static Map<String, ServerModel> getModels() {
        return MODELS;
    }

    public static boolean hasArrowModel(String modelName) {
        var info = MODELS.get(modelName);
        if (info == null) {
            return false;
        }
        return info.geoModels().contains(ModelIdUtil.ARROW_MODEL_NAME);
    }

    public static Set<String> getAuthModels() {
        return AUTH_MODELS;
    }

    // 非阻塞
    public static void syncModelsToPlayer(ServerPlayer player, @Nullable Consumer<SyncModelResult> completeCallback) {
        syncTaskEnqueue(new UUID[]{player.getUUID()}, new String[]{player.getGameProfile().getName()}, completeCallback);
    }

    public static native ExportModelResult exportModel(String modelName);

    // 非阻塞
    // 如果有其它 reload 任务正在进行，将忽略本次重载并返回 false
    public static boolean reloadAndSync(@Nullable final Consumer<ReloadModelResult> reloadCompleteCallback, @Nullable final Consumer<SyncModelResult> syncCompleteCallback) {
        return reloadBegin((Consumer<ReloadModelResult>) result -> {
            if (reloadCompleteCallback != null) {
                reloadCompleteCallback.accept(result);
            }
            final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return;
            }
            server.execute(() -> {
                Collection<ServerPlayer> players = server.getPlayerList().getPlayers();
                UUID[] uuids = players.stream().filter(NetworkHandler::isPlayerChannelPresent).map(Entity::getUUID).toArray(UUID[]::new);
                String[] playerNames = players.stream().filter(NetworkHandler::isPlayerChannelPresent).map(p -> p.getGameProfile().getName()).toArray(String[]::new);
                syncTaskEnqueue(uuids, playerNames, syncCompleteCallback);
            });
        });
    }

    // 非阻塞
    private static native boolean reloadBegin(@Nullable Object state);

    // Native Access: 在 worker 线程上调用
    @SuppressWarnings("unused,unchecked")
    private static void reloadCommit(final ReloadModelResult result, @Nullable Object state) {
        final Consumer<ReloadModelResult> completeCallback = (Consumer<ReloadModelResult>) state;
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> {
                if (result.success()) {
                    MODELS = result.models();
                    AUTH_MODELS = result.authModels();
                }
                if (completeCallback != null) {
                    ThreadTools.submit(() -> completeCallback.accept(result));
                }
            });
        } else {
            if (result.success()) {
                MODELS = result.models();
                AUTH_MODELS = result.authModels();
            }
            if (completeCallback != null) {
                completeCallback.accept(result);
            }
        }
    }

    private static native void syncTaskEnqueue(UUID[] playerIds, String[] playerNames, Object state);

    public static void syncTaskAbort(UUID playerId) {
        syncReceiveData(playerId, null);
    }

    public static native void syncReceiveData(UUID playerId, ByteBuffer data);

    // Native Access: 在 worker 线程上调用
    @SuppressWarnings("unused")
    private static boolean syncSendData(UUID playerId, ByteBuffer data) {
        return syncSendEncodedPacket(playerId, NetworkHandler.CHANNEL.toVanillaPacket(new SyncDataToClient(data), NetworkDirection.PLAY_TO_CLIENT));
    }

    // Native Access: 在 worker 线程上调用
    @SuppressWarnings("unused")
    private static Object encodeSyncDataPacket(ByteBuffer data) {
        return NetworkHandler.CHANNEL.toVanillaPacket(new SyncDataToClient(data), NetworkDirection.PLAY_TO_CLIENT);
    }

    // Native Access: 在 worker 线程上调用
    @SuppressWarnings("unused")
    private static boolean syncSendEncodedPacket(UUID playerId, Object packet) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return false;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return false;
        }

        // 不在主线程上，最好 try 一下
        try {
            player.connection.send((Packet<?>) packet);
        } catch (Exception ignored) {
            return false;
        }

        return true;
    }

    // Native Access: 在 worker 线程上调用
    @SuppressWarnings("unused,unchecked")
    private static void syncTaskComplete(final SyncModelResult result, final @Nullable Object state) {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        final Consumer<SyncModelResult> completeCallback  = (Consumer<SyncModelResult>) state;
        if (server == null) {
            if (completeCallback != null) {
                completeCallback.accept(result);
            }
            return;
        }
        server.execute(() -> {
            for (UUID playerId : result.allPlayerIds()) {
                final ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player == null) {
                    continue;
                }
                player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(modelIdCap -> {
                    player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(authModelCap -> {
                        if (authModelCap.getAuthModels().removeIf(authModel -> !MODELS.containsKey(authModel.getPath()) || !AUTH_MODELS.contains(authModel.getPath()))) {
                            NetworkHandler.sendToClientPlayer(new SyncAuthModels(authModelCap.getAuthModels()), player);
                        }

                        String modelName = modelIdCap.getModelId().getPath();
                        if (!ServerModelManager.getModels().containsKey(modelName)
                                || AUTH_MODELS.contains(modelName) && !authModelCap.containModel(modelIdCap.getModelId())
                                || !MODELS.get(modelName).textures().contains(ModelIdUtil.getSubNameFromId(modelIdCap.getSelectTexture()))) {
                            modelIdCap.setModelAndTexture(ModelIdUtil.DEFAULT_MODEL_ID, ModelIdUtil.DEFAULT_TEXTURE_ID);
                        }
                    });
                });
                if (completeCallback != null) {
                    ThreadTools.submit(() -> completeCallback.accept(result));
                }
            }
        });
    }
}
