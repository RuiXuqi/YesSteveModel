package com.elfmcys.ysm.model;

import com.elfmcys.ysm.capability.AuthModelsCapabilityProvider;
import com.elfmcys.ysm.capability.ModelInfoCapability;
import com.elfmcys.ysm.capability.ModelInfoCapabilityProvider;
import com.elfmcys.ysm.config.ServerConfig;
import com.elfmcys.ysm.network.NetworkHandler;
import com.elfmcys.ysm.network.message.SyncAuthModels;
import com.elfmcys.ysm.network.message.SyncDataToClient;
import com.elfmcys.ysm.util.ModelIdUtil;
import com.elfmcys.ysm.util.ThreadTools;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.floats.FloatReferencePair;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.server.ServerLifecycleHooks;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
    private static IntOpenHashSet MODEL_HASH_SET = new IntOpenHashSet();
    /**
     * 放置授权模型名称
     */
    private static Set<String> AUTH_MODELS = Sets.newHashSet();
    /**
     * 首次重载是否完成
     */
    private static volatile boolean INIT = false;

    public static Optional<ServerModel> getModel(String modelId) {
        return Optional.ofNullable(MODELS.get(modelId));
    }

    public static Map<String, ServerModel> getModels() {
        return MODELS;
    }

    public static Set<String> getAuthModels() {
        return AUTH_MODELS;
    }

    // 非阻塞
    @SuppressWarnings("resource")
    public static void syncModelsToPlayer(ServerPlayer player, @Nullable Consumer<SyncModelResult> completeCallback) {
        var server = ServerLifecycleHooks.getCurrentServer();
        server.execute(() -> {
            // 按距离从近到远排序
            var players = server.getPlayerList().getPlayers();
            var list = new ArrayList<FloatReferencePair<ServerPlayer>>();
            for (var onlinePlayer : players) {
                if (onlinePlayer.level().dimensionType() == player.level().dimensionType()) {
                    list.add(FloatReferencePair.of(onlinePlayer.distanceTo(player), onlinePlayer));
                }
            }
            list.sort((l, r) -> Float.compare(l.firstFloat(), r.firstFloat()));
            syncTaskEnqueue(new UUID[]{player.getUUID()}, new String[]{player.getGameProfile().getName()}, getSelectedModelIds(list.stream().map(it.unimi.dsi.fastutil.Pair::second).toList()), completeCallback);
        });
    }

    public static native void exportModel(String modelId, @Nullable String extra, @Nullable Consumer<ExportModelResult> resultCallback);

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
                for (var player : players) {
                    checkCapability(player);
                }
                UUID[] uuids = players.stream().filter(NetworkHandler::isPlayerChannelPresent).map(Entity::getUUID).toArray(UUID[]::new);
                String[] playerNames = players.stream().filter(NetworkHandler::isPlayerChannelPresent).map(p -> p.getGameProfile().getName()).toArray(String[]::new);
                String[] selectedModels = getSelectedModelIds(players);
                syncTaskEnqueue(uuids, playerNames, selectedModels, syncCompleteCallback);
            });
        });
    }

    private static String[] getSelectedModelIds(Collection<ServerPlayer> players) {
        return players.stream()
            .filter(NetworkHandler::isPlayerChannelPresent)
            .map(p -> p.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP)
                    .map(ModelInfoCapability::getModelId))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .distinct()
            .toArray(String[]::new);
    }

    // 非阻塞
    private static native boolean reloadBegin(@Nullable Object state);

    // Native Access: 在 worker 线程上调用
    @SuppressWarnings("unused,unchecked")
    private static void reloadCommit(final ReloadModelResult result, @Nullable Object state) {
        final Consumer<ReloadModelResult> completeCallback = (Consumer<ReloadModelResult>) state;
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        INIT = true;
        if (server != null) {
            server.execute(() -> {
                if (result.success()) {
                    var hashSet = new IntOpenHashSet(result.models().size());
                    for (var model : result.models().values()) {
                        hashSet.add(model.info().hashShort());
                    }
                    MODELS = result.models();
                    MODEL_HASH_SET = hashSet;
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

    private static native void syncTaskEnqueue(UUID[] playerIds, String[] playerNames, String[] selectedModels, Object state);

    public static void syncTaskAbort(UUID playerId) {
        syncReceiveData(playerId, null);
    }

    public static native void syncReceiveData(UUID playerId, ByteBuffer data);

    private static Connection getPlayerConnection(UUID playerId) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }

        var player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return null;
        }

        var conn = player.connection;
        if (!conn.isAcceptingMessages() || !conn.getClass().equals(ServerGamePacketListenerImpl.class)) {
            return null;
        }

        return conn.connection;
    }

    // Native Access: 在 worker 线程上调用
    @SuppressWarnings("unused")
    private static boolean syncSendData(UUID playerId, ByteBuffer data, TrafficContext ctx) {
        var conn = getPlayerConnection(playerId);
        if (conn != null) {
            return syncSendEncodedPacket(conn, NetworkHandler.CHANNEL.toVanillaPacket(new SyncDataToClient(data), NetworkDirection.PLAY_TO_CLIENT), ctx);
        }
        return false;
    }

    // Native Access: 在 worker 线程上调用
    @SuppressWarnings("unused")
    private static Object encodeSyncDataPacket(ByteBuffer data) {
        return NetworkHandler.CHANNEL.toVanillaPacket(new SyncDataToClient(data), NetworkDirection.PLAY_TO_CLIENT);
    }

    // Native Access: 在 worker 线程上调用
    @SuppressWarnings("unused")
    private static boolean syncSendEncodedPacket(UUID playerId, Object packet, TrafficContext ctx) {
        var conn = getPlayerConnection(playerId);
        if (conn != null) {
            return syncSendEncodedPacket(conn, packet, ctx);
        }
        return false;
    }

    private static boolean syncSendEncodedPacket(Connection conn, Object packet, TrafficContext ctx) {
        if (!ctx.init) {
            ctx.init = true;
            ctx.highWaterMark = conn.channel().unsafe().outboundBuffer().totalPendingWriteBytes() + 64 * 1024;
        }

        final AtomicInteger status = new AtomicInteger(0);
        while (conn.isConnected()) {
            if (conn.channel().unsafe().outboundBuffer().size() > ctx.highWaterMark) {
                if (!ThreadTools.safeSleep(10)) {
                    return false;
                }
                continue;
            }

            // 尝试发送
            try {
                conn.send((Packet<?>) packet, new PacketSendListener() {
                    @Override
                    public void onSuccess() {
                        status.set(1);
                        PacketSendListener.super.onSuccess();
                    }

                    @Override
                    public @Nullable Packet<?> onFailure() {
                        status.set(-1);
                        return null;
                    }
                });
            } catch (Throwable e) {
                e.printStackTrace();
                return false;
            }
            // 等待发送结束
            while (status.get() == 0) {
                if (!ThreadTools.safeSleep(5)) {
                    return false;
                }
            }
            // 发送成功返回
            if (status.get() == 1) {
                return true;
            }
            // 发送失败重试
            if (!ThreadTools.safeSleep(100)) {
                return false;
            }
            status.set(0);
        }

        return false;
    }

    public static Pair<String, String> getDefaultModelAndTexture() {
        var modelId = ServerConfig.DEFAULT_MODEL_ID.get();
        var textureName = ServerConfig.DEFAULT_MODEL_TEXTURE.get();
        if (textureName.toLowerCase().endsWith(".png") && textureName.length() > 4) {
            textureName = textureName.substring(0, textureName.length() - 4);
        }

        if (!INIT) {
            return Pair.of(modelId, textureName);
        }

        var model = MODELS.get(modelId);
        if (model == null) {
            return Pair.of(ModelIdUtil.DEFAULT_MODEL_ID, ModelIdUtil.DEFAULT_TEXTURE_NAME);
        }

        if (!model.playerModel().textures().contains(textureName)) {
            if (model.playerModel().textures().contains(model.info().properties().defaultTexture())) {
                textureName = model.info().properties().defaultTexture();
            } else {
                textureName = model.playerModel().textures().get(0);
            }
        }

        return Pair.of(modelId, textureName);
    }

    // Native Access: 在 worker 线程上调用
    @SuppressWarnings("unused,unchecked")
    private static void syncTaskComplete(final SyncModelResult result, final @Nullable Object state) {
        final Consumer<SyncModelResult> completeCallback = (Consumer<SyncModelResult>) state;
        if (completeCallback != null) {
            completeCallback.accept(result);
        }
    }

    public static void checkCapability(ServerPlayer player) {
        if (!MODELS.isEmpty()) {
            player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(modelIdCap -> {
                player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(authModelCap -> {
                    if (authModelCap.getAuthModels().removeIf(authModel -> !MODELS.containsKey(authModel) || !AUTH_MODELS.contains(authModel))) {
                        NetworkHandler.sendToClientPlayer(new SyncAuthModels(authModelCap.getAuthModels()), player);
                    }

                    String modelId = modelIdCap.getModelId();
                    if (!ServerModelManager.getModels().containsKey(modelId)
                            || (AUTH_MODELS.contains(modelId) && !authModelCap.containModel(modelIdCap.getModelId()))
                            || !MODELS.get(modelId).playerModel().textures().contains(modelIdCap.getSelectTexture())) {
                        modelIdCap.setDefault();
                    }

                    modelIdCap.trimRoamingStorage(MODEL_HASH_SET);
                });
            });
        }
    }

    // Native Access
    private static class TrafficContext {
        public long highWaterMark;
        public boolean init = false;

        // Native Access
        private TrafficContext() {
        }
    }
}
