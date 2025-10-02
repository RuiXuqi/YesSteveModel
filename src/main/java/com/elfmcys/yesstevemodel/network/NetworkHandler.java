package com.elfmcys.yesstevemodel.network;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.network.message.*;
import io.netty.util.AttributeKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("removal")
public final class NetworkHandler {
    public static final String VERSION = "2.5.1-snapshot";
    public static final ResourceLocation CHANNEL_NAME = new ResourceLocation(YesSteveModel.MOD_ID, VERSION.replace('.', '_'));
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(CHANNEL_NAME, () -> VERSION, p -> true, p -> true);
    private static final AttributeKey<String> ATTRIBUTE_CHANNEL_VERSION = AttributeKey.valueOf(YesSteveModel.MOD_ID + "_channel_version");

    public static boolean setChannelVersion(Connection connection, String channelVersion) {
        return connection.channel().attr(ATTRIBUTE_CHANNEL_VERSION).compareAndSet(null, channelVersion);
    }

    @SuppressWarnings("ConstantValue")
    public static boolean isPlayerChannelPresent(ServerPlayer player) {
        return player.connection != null && isChannelPresent(player.connection.connection);
    }

    public static boolean isRemoteChannelPresent() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return false;
        }
        return isChannelPresent(connection.getConnection());
    }

    @SuppressWarnings("ConstantValue")
    public static boolean isChannelPresent(@Nullable Connection connection) {
        // 这里冗余的判空是为了排除服务端假人
        return connection != null && connection.channel() != null && VERSION.equals(connection.channel().attr(ATTRIBUTE_CHANNEL_VERSION).get());
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
        // 10-14 包弃用
        // CHANNEL.registerMessage(10, RequestServerModelInfo.class, RequestServerModelInfo::encode, RequestServerModelInfo::decode, RequestServerModelInfo::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // CHANNEL.registerMessage(11, UploadFile.class, UploadFile::encode, UploadFile::decode, UploadFile::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        // CHANNEL.registerMessage(12, CompleteFeedback.class, CompleteFeedback::encode, CompleteFeedback::decode, CompleteFeedback::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // CHANNEL.registerMessage(13, RefreshModelManage.class, RefreshModelManage::encode, RefreshModelManage::decode, RefreshModelManage::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        // CHANNEL.registerMessage(14, HandleFile.class, HandleFile::encode, HandleFile::decode, HandleFile::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(15, SubmitRoamingVarsChanges.class, SubmitRoamingVarsChanges::encode, SubmitRoamingVarsChanges::decode, SubmitRoamingVarsChanges::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(16, SyncProjectileModelInfo.class, SyncProjectileModelInfo::encode, SyncProjectileModelInfo::decode, SyncProjectileModelInfo::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(17, SubmitRouletteConfig.class, SubmitRouletteConfig::encode, SubmitRouletteConfig::decode, SubmitRouletteConfig::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(18, EmitMolangSync.class, EmitMolangSync::encode, EmitMolangSync::decode, EmitMolangSync::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(19, MolangSync.class, MolangSync::encode, MolangSync::decode, MolangSync::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(20, DispatchRoamingVarsChanges.class, DispatchRoamingVarsChanges::encode, DispatchRoamingVarsChanges::decode, DispatchRoamingVarsChanges::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(21, DispatchServerDrivenProperty.class, DispatchServerDrivenProperty::encode, DispatchServerDrivenProperty::decode, DispatchServerDrivenProperty::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(22, SyncVehicleModelInfo.class, SyncVehicleModelInfo::encode, SyncVehicleModelInfo::decode, SyncVehicleModelInfo::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(51, ServerInfo.class, ServerInfo::encode, ServerInfo::decode, ServerInfo::handleOnClient,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(52, ClientInfo.class, ClientInfo::encode, ClientInfo::decode, ClientInfo::handleOnServer,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToServer(Object message) {
        if (isRemoteChannelPresent()) {
            CHANNEL.sendToServer(message);
        }
    }

    public static void sendToClientPlayer(Object message, final Player player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), message);
    }

    public static void broadcastToAllPlayers(Object message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }

    public static void broadcastToVisiblePlayers(Object message, final Entity centerEntity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> centerEntity), message);
    }

    public static void broadcastToVisiblePlayersAndSelf(Object message, final Player self) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> self), message);
    }
}
