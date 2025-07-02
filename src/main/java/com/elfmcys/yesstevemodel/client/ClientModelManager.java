package com.elfmcys.yesstevemodel.client;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.data.*;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.info.type.ProjectileType;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SyncDataToServer;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

// Native Access
public class ClientModelManager {
    // 以 Model Id 为索引
    private static volatile Map<String, ClientModel> MODELS = Object2ReferenceMaps.emptyMap();
    private static ClientModel DEFAULT_MODEL;
    private static final ConcurrentLinkedQueue<Triple<ClientModel, String, ObjectArrayFIFOQueue<Pair<ResourceLocation, AbstractTexture>>>> NEW_MODEL_QUEUE = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<ResourceLocation> REMOVED_TEXTURE_QUEUE = new ConcurrentLinkedQueue<>();
    private static long tickCount;

    private static final WeakHashMap<ClientModelSyncListener, Object> LISTENERS = new WeakHashMap<>();

    private static volatile Connection LAST_CONNECTION;

    public static Map<String, ClientModel> getModels() {
        return MODELS;
    }

    public static Optional<ClientModel> getModel(String modelId) {
        return Optional.ofNullable(MODELS.get(modelId));
    }

    public static ClientModel getDefaultModel() {
        return DEFAULT_MODEL;
    }

    public static Optional<Animation> getPlayerAnimation(String modelId, String animationName) {
        var model = MODELS.get(modelId);
        if (model == null) {
            return Optional.ofNullable(DEFAULT_MODEL.animations().get(animationName));
        }
        return Optional.ofNullable(model.animations().get(animationName));
    }

    public static @Nullable IValue getUserFunction(String modelId, int functionName) {
        return getModel(modelId).map(m -> m.userFunctions().get(functionName)).orElse(null);
    }

    public static @Nullable List<IValue> getMolangEventHandler(String modelId, int eventName) {
        return getModel(modelId).map(m -> m.eventHandlers().get(eventName)).orElse(null);
    }

    public static Optional<ProjectileModel> getProjectileModel(String modelId, ProjectileType type) {
        var model = MODELS.get(modelId);
        if (model == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(model.projectileModels().get(type));
    }

    public static Optional<ResourceLocation> getPlayerTextureLocation(String modelId, String textureName) {
        var model = MODELS.get(modelId);
        if (model == null) {
            return Optional.of(ModelIdUtil.DEFAULT_TEXTURE_LOCATION);
        }
        return Optional.ofNullable(model.textures().get(textureName));
    }

    // listener 以弱引用的方式存储，需要自己 hold 一个强引用防止被回收
    public static Object addSyncListener(ClientModelSyncListener listener) {
        LISTENERS.put(listener, null);
        return listener;
    }

    // 可以不显式调用，会由 GC 自动回收
    public static void removeSyncListener(ClientModelSyncListener listener) {
        LISTENERS.remove(listener, null);
    }

    public static void syncAbort() {
        syncReceiveData(null);
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
        for (var listener : LISTENERS.keySet()) {
            try {
                listener.onReceiveServerInfo();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    // Native Access
    @SuppressWarnings("unused")
    private static void syncPreparation(int total) {
        Minecraft.getInstance().execute(() -> {
            for (var listener : LISTENERS.keySet()) {
                try {
                    if (total == -1) {
                        listener.onSyncPreparation();
                    } else {
                        listener.onSyncProgression(total, 0);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }

    // Native Access
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
                        model.clientModelInfo().setNeedAuth(needAuth[i]);
                        models.put(dstModelIds[i], model);
                    }
                }
            }

            MODELS = models;

            if ((removedModelIds != null && removedModelIds.length > 0) || (alterModelIds != null && alterModelIds.length > 0)) {
                for (var listener : LISTENERS.keySet()) {
                    try {
                        listener.onAlterModels(models);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        });
    }

    // Native Access
    @SuppressWarnings("unused")
    private static void addModel(ClientModelData modelData, String modelId, boolean isDefault, boolean isNeedAuth, int total, int received) {
        ClientModel model;
        var textures = new ObjectArrayFIFOQueue<Pair<ResourceLocation, AbstractTexture>>(4);
        try {
            model = ClientModelBuilder.build(modelData, isDefault, isNeedAuth, textures);
        } catch (Exception e) {
            if (isDefault) {
                throw e;
            }
            YesSteveModel.LOGGER.error(new StringFormattedMessage("Failed to process {}", modelId), e);
            return;
        }
        NEW_MODEL_QUEUE.add(new ImmutableTriple<>(model, modelId, textures));
        if (isDefault) {
            DEFAULT_MODEL = model;
        } else if (total != -1) {
            Minecraft.getInstance().execute(() -> {
                for (var listener : LISTENERS.keySet()) {
                    try {
                        listener.onSyncProgression(total, received);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            });
        }
    }

    // Native Access
    @SuppressWarnings("unused")
    private static void syncAborted() {
        Minecraft.getInstance().execute(() -> {
            for (var listener : LISTENERS.keySet()) {
                try {
                    listener.onSyncAbort();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }

    // Native Access
    @SuppressWarnings("unused")
    private static void syncFailed(@Nullable Object msg) {
        Minecraft.getInstance().execute(() -> {
            if (msg instanceof Component component) {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(component);
                }
                YesSteveModel.LOGGER.error(component.getString(256));
            }
            for (var listener : LISTENERS.keySet()) {
                try {
                    listener.onSyncFailed(msg == null ? null : (Component) msg);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }

    // 每两 tick 注册或移除一个贴图，尽可能避免卡顿
    public static void tick() {
        if (++tickCount % 2 == 1) {
            return;
        }
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
            var texturePair = newModelPair.getRight().dequeue();
            Minecraft.getInstance().getTextureManager().register(texturePair.getLeft(), texturePair.getRight());
            return;
        }
        NEW_MODEL_QUEUE.poll();
        var models = new Object2ReferenceOpenHashMap<>(MODELS);
        models.put(newModelPair.getMiddle(), newModelPair.getLeft());
        MODELS = models;

        for (var listener : LISTENERS.keySet()) {
            try {
                listener.onNewModelLoaded(models, newModelPair.getMiddle(), newModelPair.getLeft());
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
