package com.elfmcys.yesstevemodel.network;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.network.message.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class NetworkHandler {
    private static final String VERSION = "1.1.0";
    private static final ResourceLocation CHANNEL_NAME = new ResourceLocation(YesSteveModel.MOD_ID, VERSION);
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(CHANNEL_NAME, () -> VERSION,
            NetworkHandler::checkProtocolVersion, NetworkHandler::checkProtocolVersion);

    private static boolean checkProtocolVersion(String protocolVersionIn) {
        // 都安装 YSM 的情况下要求版本相同
        if (protocolVersionIn.equals(VERSION)) {
            return true;
        }
        // 允许其中一方未安装 YSM
        if (protocolVersionIn.equals(NetworkRegistry.ABSENT)) {
            return true;
        }
        // 允许其中一方是原版端
        if (protocolVersionIn.equals(NetworkRegistry.ACCEPTVANILLA)) {
            return true;
        }
        return false;
    }

    // 检测客户端是否安装了相同版本的 YSM 模组
    public static boolean isPlayerChannelPresent(ServerPlayer player) {
        ConnectionData connectionData = NetworkHooks.getConnectionData(player.connection.connection);
        // 原版端
        if (connectionData == null) {
            return false;
        }
        String channelVersion = connectionData.getChannels().get(CHANNEL_NAME);
        // 未安装 YSM 或版本不匹配
        if (!VERSION.equals(channelVersion)) {
            return false;
        }

        return true;
    }

    public static void init() {
        CHANNEL.registerMessage(1, SyncDataToClient.class, SyncDataToClient::encode, SyncDataToClient::decode, SyncDataToClient::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, SyncDataToServer.class, SyncDataToServer::encode, SyncDataToServer::decode, SyncDataToServer::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(3, ExecuteMolang.class, ExecuteMolang::encode, ExecuteMolang::decode, ExecuteMolang::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(4, SyncModelInfo.class, SyncModelInfo::encode, SyncModelInfo::decode, SyncModelInfo::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(5, SetModelAndTexture.class, SetModelAndTexture::encode, SetModelAndTexture::decode, SetModelAndTexture::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(6, SyncAuthModels.class, SyncAuthModels::encode, SyncAuthModels::decode, SyncAuthModels::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(7, SetPlayAnimation.class, SetPlayAnimation::encode, SetPlayAnimation::decode, SetPlayAnimation::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(8, SyncStarModels.class, SyncStarModels::encode, SyncStarModels::decode, SyncStarModels::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(9, SetStarModel.class, SetStarModel::encode, SetStarModel::decode, SetStarModel::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(10, RequestServerModelInfo.class, RequestServerModelInfo::encode, RequestServerModelInfo::decode, RequestServerModelInfo::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(11, UploadFile.class, UploadFile::encode, UploadFile::decode, UploadFile::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(12, CompleteFeedback.class, CompleteFeedback::encode, CompleteFeedback::decode, CompleteFeedback::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(13, RefreshModelManage.class, RefreshModelManage::encode, RefreshModelManage::decode, RefreshModelManage::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(14, HandleFile.class, HandleFile::encode, HandleFile::decode, HandleFile::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToClientPlayer(Object message, final Player player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), message);
    }

    public static void broadcastToAllPlayers(Object message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }

    public static void broadcastToVisiblePlayersAndSelf(Object message, final Player self) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> self), message);
    }
}
