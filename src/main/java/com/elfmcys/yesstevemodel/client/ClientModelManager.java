package com.elfmcys.yesstevemodel.client;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.data.ClientModelSyncResult;
import com.elfmcys.yesstevemodel.client.data.ProjectileModel;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.info.type.ProjectileType;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SyncDataToServer;
import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Native Access
public class ClientModelManager {
    // 以 Model Id 为索引
    private static Map<String, ClientModel> MODELS = Maps.newHashMap();
    private static ClientModel DEFAULT_MODEL;

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
            return Optional.empty();
        }
        return Optional.ofNullable(model.animations().get(animationName));
    }

    public static @Nullable IValue getUserFunction(String modelId, int functionName) {
        return getModel(modelId).map(m -> m.functions().get(functionName)).orElse(null);
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
            return Optional.empty();
        }
        return Optional.ofNullable(model.textures().get(textureName));
    }

    public static void syncAbort() {
        syncReceiveData(null);
    }

    // Native Access: 在 worker 线程上调用
    @SuppressWarnings("unused")
    private static void syncCommit(final ClientModelSyncResult result) {
        // Render Thread 单线程模型提供天然的原子性
        Minecraft.getInstance().execute(() -> {
            if (result.message != null && Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(result.message);
                YesSteveModel.LOGGER.error(result.message.getString(256));
            }
            if (!result.success) {
                return;
            }
            MODELS = result.models;
            if (result.defaultModel != null) {
                DEFAULT_MODEL = result.defaultModel;
            }
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
}
