package com.elfmcys.yesstevemodel.client;

import com.elfmcys.yesstevemodel.client.animation.condition.ConditionManager;
import com.elfmcys.yesstevemodel.client.data.ClientModelInfo;
import com.elfmcys.yesstevemodel.client.data.ClientModelSyncResult;
import com.elfmcys.yesstevemodel.client.gui.ModelManageScreen;
import com.elfmcys.yesstevemodel.geckolib3.file.AnimationFile;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.resource.GeckoLibCache;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.RequestServerModelInfo;
import com.elfmcys.yesstevemodel.network.message.SyncDataToServer;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Native Access
public class ClientModelManager {
    // 以 Model Id 为索引
    private static Map<ResourceLocation, ClientModelInfo> MODEL_INFO = Maps.newHashMap();
    // 存储 Model Name
    private static Set<String> AUTH_MODEL_NAMES = Sets.newHashSet();
    private static AnimationFile DEFAULT_ANIMATION_FILE = new AnimationFile(Maps.newHashMap());
    private static GeoModel DEFAULT_MAIN_MODEL;

    private static volatile Connection LAST_CONNECTION;

    public static Set<String> getAuthModelNames() {
        return AUTH_MODEL_NAMES;
    }

    public static Map<ResourceLocation, ClientModelInfo> getModelInfo() {
        return MODEL_INFO;
    }

    public static AnimationFile getDefaultAnimationFile() {
        return DEFAULT_ANIMATION_FILE;
    }

    public static GeoModel getDefaultMainModel() {
        return DEFAULT_MAIN_MODEL;
    }

    public static void syncAbort() {
        syncReceiveData(null);
    }

    // Native Access: 在 worker 线程上调用
    @SuppressWarnings("unused")
    private static void syncCommit(final ClientModelSyncResult result) {
        // Render Thread 单线程模型提供天然的原子性
        Minecraft.getInstance().execute(() -> {
            if(result.message != null && Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(result.message);
            }
            if (!result.success) {
                return;
            }
            MODEL_INFO = result.modelInfo;
            AUTH_MODEL_NAMES = result.authModels;
            GeckoLibCache.getInstance().setAll(result.geoModels, result.animations);
            ConditionManager.setInstance(result.conditionManager);
            DEFAULT_ANIMATION_FILE = result.defaultMainAnimationFile;
            DEFAULT_MAIN_MODEL = result.defaultMainModel;
            result.releaseRemovedTextures();
            result.replaceDuplicatedTextures();
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
