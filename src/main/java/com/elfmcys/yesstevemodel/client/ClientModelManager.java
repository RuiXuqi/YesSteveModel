package com.elfmcys.yesstevemodel.client;

import com.elfmcys.yesstevemodel.client.animation.condition.ConditionManager;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.data.ClientModelInfo;
import com.elfmcys.yesstevemodel.client.data.ClientModelSyncResult;
import com.elfmcys.yesstevemodel.client.gui.ModelManageScreen;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.RequestServerModelInfo;
import com.elfmcys.yesstevemodel.network.message.SyncDataToServer;
import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
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

    public static Optional<ClientModel> getModel(String modelName) {
        return Optional.ofNullable(MODELS.get(modelName));
    }

    public static ClientModel getDefaultModel() {
        return DEFAULT_MODEL;
    }

    public static Optional<Animation> getPlayerAnimation(String modelName, String animationName) {
        var model = MODELS.get(modelName);
        if (model == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(model.mainAnimations().get(animationName));
    }

    public static Optional<Animation> getArrowAnimation(String modelName, String animationName) {
        var model = MODELS.get(modelName);
        if (model == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(model.arrowAnimations().get(animationName));
    }

    public static boolean isModelNeedAuth(String modelName) {
        var model = MODELS.get(modelName);
        if (model == null) {
            return false;
        }
        return model.clientModelInfo().isNeedAuth();
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
            }
            if (!result.success) {
                return;
            }
            MODELS = result.models;
            if (result.defaultModel != null) {
                DEFAULT_MODEL = result.defaultModel;
            }
            ConditionManager.setInstance(result.conditionManager);
            result.releaseRemovedTextures();
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

    public static void openModelManageScreen(List<RequestServerModelInfo.Info> customModels, List<RequestServerModelInfo.Info> authModels) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ModelManageScreen(customModels, authModels));
    }
}
