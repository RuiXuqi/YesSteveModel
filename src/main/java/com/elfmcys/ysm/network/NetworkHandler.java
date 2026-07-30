package com.elfmcys.ysm.network;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.network.forge.AssetTransferHandler;
import com.elfmcys.ysm.network.forge.ClientboundEnvelope;
import com.elfmcys.ysm.network.forge.ControlHandler;
import com.elfmcys.ysm.network.forge.ForgeEnvelope;
import com.elfmcys.ysm.network.forge.ForgeMessageBinding;
import com.elfmcys.ysm.network.forge.ForgeProtoCodec;
import com.elfmcys.ysm.network.forge.ForgeProtocolRegistry;
import com.elfmcys.ysm.network.forge.HandshakeHandler;
import com.elfmcys.ysm.network.forge.MinecraftStateHandler;
import com.elfmcys.ysm.network.forge.PlayerStateHandler;
import com.elfmcys.ysm.network.forge.ServerboundEnvelope;
import com.elfmcys.ysm.network.protocol.MessageDirection;
import com.elfmcys.ysm.network.protocol.PeerProtocolProfile;
import com.elfmcys.ysm.network.protocol.ProtocolMessageSpec;
import com.elfmcys.ysm.network.protocol.ProtocolVersion;
import com.elfmcys.ysm.network.protocol.ProtocolMessages;
import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;
import com.elfmcys.ysm.proto.network.protocol.v0.ControlV0;
import com.elfmcys.ysm.proto.network.protocol.v0.HandshakeV0;
import com.elfmcys.ysm.proto.network.protocol.v0.PlayerStateV0;
import com.elfmcys.ysm.proto.network.protocol.v0.minecraft.MinecraftStateV0;
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
import us.hebi.quickbuf.ProtoMessage;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@SuppressWarnings("removal")
public final class NetworkHandler {
    public static final String VERSION = ProtocolVersion.TRANSPORT_VERSION;
    public static final ResourceLocation CHANNEL_NAME =
            new ResourceLocation(YesSteveModel.MOD_ID, ProtocolVersion.CHANNEL_PATH);
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(CHANNEL_NAME, () -> VERSION,
            NetworkHandler::acceptsTransportVersion, NetworkHandler::acceptsTransportVersion);
    private static final AttributeKey<String> ATTRIBUTE_CHANNEL_VERSION =
            AttributeKey.valueOf(YesSteveModel.MOD_ID + "_channel_version");
    private static final AttributeKey<PeerProtocolProfile> ATTRIBUTE_PEER_PROFILE =
            AttributeKey.valueOf(YesSteveModel.MOD_ID + "_peer_protocol_profile");

    private NetworkHandler() {
    }

    public static boolean setChannelVersion(Connection connection, String channelVersion) {
        return connection.channel().attr(ATTRIBUTE_CHANNEL_VERSION).compareAndSet(null, channelVersion);
    }

    public static boolean setPeerProfile(Connection connection, PeerProtocolProfile profile) {
        return connection.channel().attr(ATTRIBUTE_PEER_PROFILE).compareAndSet(null, profile);
    }

    public static Optional<PeerProtocolProfile> peerProfile(@Nullable Connection connection) {
        return connection == null || connection.channel() == null
                ? Optional.empty() : Optional.ofNullable(connection.channel().attr(ATTRIBUTE_PEER_PROFILE).get());
    }

    @SuppressWarnings("ConstantValue")
    public static boolean isPlayerChannelPresent(ServerPlayer player) {
        return player.connection != null && isChannelPresent(player.connection.connection);
    }

    public static boolean isRemoteChannelPresent() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && isChannelPresent(connection.getConnection());
    }

    @SuppressWarnings("ConstantValue")
    public static boolean isChannelPresent(@Nullable Connection connection) {
        return connection != null && connection.channel() != null
                && VERSION.equals(connection.channel().attr(ATTRIBUTE_CHANNEL_VERSION).get());
    }

    public static void init() {
        registerProto(HandshakeV0.ClientHello.class, HandshakeV0.ClientHello::parseFrom,
                HandshakeHandler::handleClientHello);
        registerProto(HandshakeV0.ServerHello.class, HandshakeV0.ServerHello::parseFrom,
                HandshakeHandler::handleServerHello);
        registerProto(PlayerStateV0.PlayerStateReport.class, PlayerStateV0.PlayerStateReport::parseFrom,
                PlayerStateHandler::handleReport);
        registerProto(PlayerStateV0.PlayerStateUpdate.class, PlayerStateV0.PlayerStateUpdate::parseFrom,
                PlayerStateHandler::handleUpdate);
        registerProto(ControlV0.SelectModelRequest.class, ControlV0.SelectModelRequest::parseFrom,
                ControlHandler::handleSelectModel);
        registerProto(ControlV0.AuthorizedModelsSnapshot.class, ControlV0.AuthorizedModelsSnapshot::parseFrom,
                ControlHandler::handleAuthorizedModels);
        registerProto(ControlV0.StarredModelsSnapshot.class, ControlV0.StarredModelsSnapshot::parseFrom,
                ControlHandler::handleStarredModels);
        registerProto(ControlV0.UpdateStarredModelRequest.class, ControlV0.UpdateStarredModelRequest::parseFrom,
                ControlHandler::handleUpdateStar);
        registerProto(ControlV0.EntityAnimationActionRequest.class, ControlV0.EntityAnimationActionRequest::parseFrom,
                ControlHandler::handleEntityAnimation);
        registerProto(ControlV0.ExecuteMolangEvent.class, ControlV0.ExecuteMolangEvent::parseFrom,
                ControlHandler::handleExecuteMolang);
        registerProto(ControlV0.SubmitRouletteExpressionRequest.class,
                ControlV0.SubmitRouletteExpressionRequest::parseFrom, ControlHandler::handleSubmitRoulette);
        registerProto(ControlV0.EmitMolangSync.class, ControlV0.EmitMolangSync::parseFrom,
                ControlHandler::handleEmitMolangSync);
        registerProto(ControlV0.MolangSyncEvent.class, ControlV0.MolangSyncEvent::parseFrom,
                ControlHandler::handleMolangSync);
        registerProto(ControlV0.SwingHandRequest.class, ControlV0.SwingHandRequest::parseFrom,
                ControlHandler::handleSwingHand);
        registerProto(MinecraftStateV0.ProjectileModelState.class, MinecraftStateV0.ProjectileModelState::parseFrom,
                MinecraftStateHandler::handleProjectile);
        registerProto(MinecraftStateV0.VehicleModelState.class, MinecraftStateV0.VehicleModelState::parseFrom,
                MinecraftStateHandler::handleVehicle);
        register(AssetTransferV0.AssetFragment.class, AssetTransferV0.AssetFragment::parseFrom,
                AssetTransferHandler::handleFragmentPayload);
        registerProto(AssetTransferV0.ModelAssetBatchRequest.class, AssetTransferV0.ModelAssetBatchRequest::parseFrom,
                AssetTransferHandler::handleBatchRequest);
        registerProto(AssetTransferV0.ModelAssetBatchFailure.class, AssetTransferV0.ModelAssetBatchFailure::parseFrom,
                AssetTransferHandler::handleBatchFailure);
        registerProto(AssetTransferV0.CatalogResyncRequest.class, AssetTransferV0.CatalogResyncRequest::parseFrom,
                AssetTransferHandler::handleCatalogResync);
        registerProto(AssetTransferV0.ModelAssetBatchCancel.class, AssetTransferV0.ModelAssetBatchCancel::parseFrom,
                AssetTransferHandler::handleBatchCancel);
        registerProto(AssetTransferV0.AssetTransferRelease.class, AssetTransferV0.AssetTransferRelease::parseFrom,
                AssetTransferHandler::handleTransferRelease);

        CHANNEL.registerMessage(0, ServerboundEnvelope.class, ForgeProtoCodec::encode,
                buffer -> (ServerboundEnvelope) ForgeProtoCodec.decode(
                        buffer, MessageDirection.CLIENT_TO_SERVER),
                ForgeEnvelope::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, ClientboundEnvelope.class, ForgeProtoCodec::encode,
                buffer -> (ClientboundEnvelope) ForgeProtoCodec.decode(
                        buffer, MessageDirection.SERVER_TO_CLIENT),
                ForgeEnvelope::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private static <T extends ProtoMessage<T>> void registerProto(
            Class<T> type, ForgeProtoCodec.Parser<T> parser,
            BiConsumer<T, Supplier<net.minecraftforge.network.NetworkEvent.Context>> handler) {
        register(type, parser, (payload, context) -> {
            try (payload) {
                if (payload.raw().isPresent()) {
                    throw new IllegalArgumentException("Raw attachment is not valid for " + type.getName());
                }
                handler.accept(payload.protobuf(), context);
            }
        });
    }

    private static <T extends ProtoMessage<T>> void register(
            Class<T> type, ForgeProtoCodec.Parser<T> parser,
            BiConsumer<NetworkPayload<T>, Supplier<net.minecraftforge.network.NetworkEvent.Context>> handler) {
        ProtocolMessageSpec<T> spec = ProtocolMessages.REGISTRY.find(type).orElseThrow();
        ForgeProtocolRegistry.add(new ForgeMessageBinding<>(spec, parser, handler));
    }

    private static boolean acceptsTransportVersion(String version) {
        return VERSION.equals(version) || NetworkRegistry.ABSENT.equals(version);
    }

    public static void sendToServer(ProtoMessage<?> message) {
        sendToServer(payload(message));
    }

    public static void sendToServer(NetworkPayload<?> payload) {
        if (!isRemoteChannelPresent()) {
            payload.close();
            return;
        }
        var envelope = serverbound(payload);
        try {
            CHANNEL.sendToServer(envelope);
        } catch (Throwable error) {
            envelope.close();
            throw error;
        }
    }

    public static void sendToClientPlayer(ProtoMessage<?> message, Player player) {
        sendToClientPlayer(payload(message), player);
    }

    public static void sendToClientPlayer(NetworkPayload<?> payload, Player player) {
        var envelope = clientbound(payload);
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), envelope);
        } catch (Throwable error) {
            envelope.close();
            throw error;
        }
    }

    public static void broadcastToAllPlayers(ProtoMessage<?> message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), clientbound(payload(message)));
    }

    public static void broadcastToVisiblePlayers(ProtoMessage<?> message, Entity centerEntity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> centerEntity), clientbound(payload(message)));
    }

    public static void broadcastToVisiblePlayersAndSelf(ProtoMessage<?> message, Player self) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> self), clientbound(payload(message)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static NetworkPayload<?> payload(ProtoMessage<?> message) {
        return NetworkPayload.protobuf((ProtoMessage) message);
    }

    private static ServerboundEnvelope serverbound(NetworkPayload<?> payload) {
        return new ServerboundEnvelope(binding(payload, MessageDirection.CLIENT_TO_SERVER), payload, true);
    }

    private static ClientboundEnvelope clientbound(NetworkPayload<?> payload) {
        return new ClientboundEnvelope(binding(payload, MessageDirection.SERVER_TO_CLIENT), payload, true);
    }

    private static ForgeMessageBinding<?> binding(NetworkPayload<?> payload, MessageDirection direction) {
        var spec = ProtocolMessages.REGISTRY.find(payload.protobuf().getClass())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unregistered protocol message type: " + payload.protobuf().getClass().getName()));
        if (spec.direction() != direction) {
            throw new IllegalArgumentException("Protocol message has the wrong network direction");
        }
        return ForgeProtocolRegistry.find(spec.id(), direction).orElseThrow();
    }
}
