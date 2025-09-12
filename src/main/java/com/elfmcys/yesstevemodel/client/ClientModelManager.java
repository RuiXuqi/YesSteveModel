package com.elfmcys.yesstevemodel.client;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.model.*;
import com.elfmcys.yesstevemodel.client.model.data.ClientModelData;
import com.elfmcys.yesstevemodel.client.texture.NativeTexture;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SyncDataToServer;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

// Native Access
public class ClientModelManager {
    // 以 Model Path 为索引
    private static volatile Map<String, ClientModel> MODELS = Object2ReferenceMaps.emptyMap();
    // 以 Pack Path 为索引
    private static volatile Map<String, ModelPackInfo> PACKS = new Object2ReferenceOpenHashMap<>();

    private static ClientModel DEFAULT_MODEL;

    private static final ConcurrentLinkedQueue<Triple<ClientModel, String, List<Pair<ResourceLocation, AbstractTexture>>>> NEW_MODEL_QUEUE = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<ResourceLocation> REMOVED_TEXTURE_QUEUE = new ConcurrentLinkedQueue<>();

    private static final WeakHashMap<ClientModelSyncListener, Object> LISTENERS = new WeakHashMap<>();

    private static final SyncState SYNC_STATE = new SyncState();
    private static volatile Connection LAST_CONNECTION;

    public static SyncState getSyncState() {
        RenderSystem.assertOnGameThread();
        return SYNC_STATE;
    }

    public static Map<String, ClientModel> getModels() {
        return MODELS;
    }

    public static Map<String, ModelPackInfo> getPacks() {
        return PACKS;
    }

    public static Optional<ClientModel> getModel(String modelId) {
        return Optional.ofNullable(MODELS.get(modelId));
    }

    public static ClientModel getDefaultModel() {
        return DEFAULT_MODEL;
    }

    // listener 以弱引用的方式存储，需要自己 hold 一个强引用防止被回收
    @SuppressWarnings("UnusedReturnValue")
    public static <T extends ClientModelSyncListener> T addSyncListener(T listener) {
        LISTENERS.put(listener, null);
        return listener;
    }

    // 可以不显式调用，会由 GC 自动回收
    public static void removeSyncListener(ClientModelSyncListener listener) {
        LISTENERS.remove(listener, null);
    }

    private static void invokeListener(Consumer<ClientModelSyncListener> consumer) {
        for (var listener : LISTENERS.keySet()) {
            try {
                consumer.accept(listener);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public static void syncAbort() {
        syncReceiveData(null);
        Minecraft.getInstance().execute(() -> {
            SYNC_STATE.setType(SyncStateType.WAITING);
        });
    }

    // Native Access: 在 worker 线程上调用
    @SuppressWarnings("unused")
    private static void syncSendData(ByteBuffer data) {
        if (Minecraft.getInstance().player != null) {
            try {
                NetworkHandler.CHANNEL.sendToServer(new SyncDataToServer(data));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            var connection = LAST_CONNECTION;
            if (!connection.isConnected()) {
                return;
            }
            try {
                Packet<?> packet = NetworkHandler.CHANNEL.toVanillaPacket(new SyncDataToServer(data), NetworkDirection.PLAY_TO_SERVER);
                connection.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 只能接收 DirectByteBuffer
    public static void syncReceiveData(Connection connection, ByteBuffer data) {
        LAST_CONNECTION = connection;
        syncReceiveData(data);
    }

    // 只能接收 DirectByteBuffer
    private static native void syncReceiveData(ByteBuffer data);

    public static void receiveServerInfo() {
        if (Minecraft.getInstance().isLocalServer()) {
            SYNC_STATE.setType(SyncStateType.LOADING);
        } else {
            SYNC_STATE.setType(SyncStateType.IDLE);
        }
        invokeListener(ClientModelSyncListener::onReceiveServerInfo);
    }

    // Native Access
    @SuppressWarnings("unused")
    private static void syncPreparation(int total) {
        if (total == -1) {
            Minecraft.getInstance().execute(() -> {
                SYNC_STATE.setType(SyncStateType.PREPARING);
                invokeListener(ClientModelSyncListener::onSyncPreparation);
            });
        } else {
            Minecraft.getInstance().execute(() -> {
                if (total > 0) {
                    SYNC_STATE.setSyncing(total);
                } else {
                    SYNC_STATE.setType(SyncStateType.IDLE);
                }
                invokeListener(listener -> listener.onSyncProgression(total, 0));
            });
        }
    }

    /**
     * 每次同步开始时调用，
     * 注意不在主线程上。
     */
    // Native Access
    @SuppressWarnings("unused")
    private static void updateModelPackInfo(ModelPackInfo[] list) {
        var packs = new Object2ReferenceOpenHashMap<String, ModelPackInfo>();
        for (var pack : list) {
            if (StringUtils.isBlank(pack.name())) {
                pack = new ModelPackInfo(
                        pack.hierarchy(),
                        ModelIdUtil.getLastFolderName(pack.hierarchy()),
                        pack.desc(),
                        pack.icon(),
                        pack.lang()
                );
            }
            packs.put(pack.hierarchy(), pack);
            final NativeTexture icon = pack.icon();
            if (icon != null) {
                final ResourceLocation id = ModelIdUtil.getModelPackIconId(pack.hierarchy());
                Minecraft.getInstance().submit(() -> {
                    Minecraft.getInstance().textureManager.register(id, icon);
                });
            }
        }

        for (var oldPack : PACKS.values()) {
            if (!packs.containsKey(oldPack.hierarchy()) && oldPack.icon() != null) {
                final ResourceLocation id = ModelIdUtil.getModelPackIconId(oldPack.hierarchy());
                Minecraft.getInstance().submit(() -> Minecraft.getInstance().textureManager.release(id));
            }
        }

        PACKS = packs;
    }

    // Native Access
    // 在 model pack 同步之后调用，注意不在主线程上
    @SuppressWarnings("all")
    private static void alterModel(
            String @Nullable [] removedModelIds,
            String @Nullable [] alterModelIds,
            String @Nullable [] dstModelIds,
            boolean @Nullable [] needAuth) {
        Minecraft.getInstance().execute(() -> {
            var models = new Object2ReferenceOpenHashMap<>(MODELS);

            if (removedModelIds != null) {
                for (String removedModelId : removedModelIds) {
                    var removedModel = models.remove(removedModelId);
                    if (removedModel != null) {
                        REMOVED_TEXTURE_QUEUE.addAll(removedModel.registeredTextureIds());
                    }
                }
            }

            if (alterModelIds != null) {
                ClientModel[] alterModels = new ClientModel[alterModelIds.length];
                for (var i = 0; i < alterModelIds.length; i++) {
                    alterModels[i] = models.remove(alterModelIds[i]);
                }
                for (var i = 0; i < alterModels.length; i++) {
                    var model = alterModels[i];
                    if (model != null) {
                        model.clientInfo().setNeedAuth(needAuth[i]);
                        models.put(dstModelIds[i], model);
                    }
                }
            }

            MODELS = models;

            if ((removedModelIds != null && removedModelIds.length > 0)
                || (alterModelIds != null && alterModelIds.length > 0)) {
                invokeListener(listener -> listener.onAlterModels(models));
            }
        });
    }

    // Native Access
    @SuppressWarnings("unused")
    private static void addModel(ClientModelData modelData, String modelPath, boolean isDefault, boolean isNeedAuth) {
        ClientModel model;
        var textures = new ArrayList<Pair<ResourceLocation, AbstractTexture>>(4);
        try {
            model = ClientModelBuilder.build(modelData, isDefault, isNeedAuth, textures);
        } catch (Exception e) {
            if (isDefault) {
                throw e;
            }
            YesSteveModel.LOGGER.error(new StringFormattedMessage("Failed to process {}", modelPath), e);
            return;
        }
        NEW_MODEL_QUEUE.add(new ImmutableTriple<>(model, modelPath, textures));
        if (isDefault) {
            DEFAULT_MODEL = model;
            return;
        }

        Minecraft.getInstance().execute(() -> {
            if (SYNC_STATE.type == SyncStateType.SYNCING) {
                var received = ++SYNC_STATE.received;
                if (received == SYNC_STATE.total) {
                    SYNC_STATE.setType(SyncStateType.IDLE);
                }
                invokeListener(listener -> listener.onSyncProgression(SYNC_STATE.getTotal(), received));
            }
        });
    }

    // Native Access
    @SuppressWarnings("unused")
    private static void syncAborted() {
        Minecraft.getInstance().execute(() -> {
            SYNC_STATE.setType(SyncStateType.IDLE);
            invokeListener(ClientModelSyncListener::onSyncAbort);
        });
    }

    // Native Access
    @SuppressWarnings("unused")
    private static void syncFailed(@Nullable Object msg) {
        Minecraft.getInstance().execute(() -> {
            SYNC_STATE.setType(SyncStateType.IDLE);
            invokeListener(listener -> listener.onSyncFailed(msg == null ? null : (Component) msg));
            if (msg instanceof Component component) {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(component);
                }
                YesSteveModel.LOGGER.error(component.getString(256));
            }
        });
    }

    // 每 tick 注册或移除一个贴图，尽可能避免卡顿
    public static void tick() {
        var removed = REMOVED_TEXTURE_QUEUE.poll();
        if (removed != null) {
            Minecraft.getInstance().getTextureManager().release(removed);
            return;
        }

        var newModelPair = NEW_MODEL_QUEUE.peek();
        if (newModelPair == null) {
            return;
        }

        if (!newModelPair.getRight().isEmpty()) {
            var textureList = newModelPair.getRight();
            var texturePair = textureList.remove(textureList.size() - 1);
            Minecraft.getInstance().getTextureManager().register(texturePair.getLeft(), texturePair.getRight());
            return;
        }
        NEW_MODEL_QUEUE.poll();

        var models = new Object2ReferenceOpenHashMap<>(MODELS);
        String modelPath = newModelPair.getMiddle();
        models.put(modelPath, newModelPair.getLeft());

        MODELS = models;
        invokeListener(listener ->
                listener.onNewModelLoaded(models, modelPath, newModelPair.getLeft()));
    }

    public static class SyncState {
        private SyncStateType type = SyncStateType.WAITING;
        private int total = -1;
        private int received = -1;

        public SyncStateType getType() {
            return type;
        }

        public int getReceived() {
            return received;
        }

        public int getTotal() {
            return total;
        }

        public void setType(SyncStateType type) {
            this.type = type;
            this.total = -1;
            this.received = -1;
        }

        public void setSyncing(int total) {
            this.type = SyncStateType.SYNCING;
            this.total = total;
            this.received = 0;
        }
    }

    public enum SyncStateType {
        WAITING,
        LOADING,
        IDLE,
        PREPARING,
        SYNCING,
    }
}
