package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.capability.AuthModelsCapabilityProvider;
import com.elfmcys.ysm.capability.ModelInfoCapabilityProvider;
import com.elfmcys.ysm.capability.StarModelsCapabilityProvider;
import com.elfmcys.ysm.client.model.ClientModelService;
import com.elfmcys.ysm.model.server.ServerModelService;
import com.elfmcys.ysm.network.NetworkHandler;
import com.elfmcys.ysm.network.protocol.PeerProtocolProfile;
import com.elfmcys.ysm.network.protocol.ProtocolVersion;
import com.elfmcys.ysm.network.protocol.ProtocolLimits;
import com.elfmcys.ysm.network.protocol.ProtocolProfiles;
import com.elfmcys.ysm.network.protocol.ProtocolPolicies;
import com.elfmcys.ysm.network.protocol.PlayerIdTransmissionMode;
import com.elfmcys.ysm.network.protocol.DefaultAnimationNegotiation;
import com.elfmcys.ysm.proto.network.protocol.v0.HandshakeV0;
import com.elfmcys.ysm.config.ServerConfig;
import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.model.ModelRuntime;
import com.elfmcys.ysm.model.catalog.DefaultAnimationKey;
import net.minecraftforge.fml.ModList;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
import java.util.Set;

public final class HandshakeHandler {
    private HandshakeHandler() {
    }

    public static HandshakeV0.ServerHello createServerHello() {
        var reportPolicy = HandshakeV0.PlayerStateReportPolicy.newInstance()
                .setMinDeltaIntervalMs(50)
                .setFullSnapshotIntervalMs(30_000)
                .addRequestedSections(HandshakeV0.PlayerStateSection.PLAYER_STATE_SECTION_ANIMATION);
        if (!ServerConfig.LOW_BANDWIDTH_USAGE.get()) {
            reportPolicy.addRequestedSections(HandshakeV0.PlayerStateSection.PLAYER_STATE_SECTION_ROAMING);
        }
        var result = HandshakeV0.ServerHello.newInstance()
                .setProtocolVersion(ProtocolVersion.CURRENT)
                .addEnabledFeatureIds(ProtocolVersion.ASSET_TRANSFER_RELEASE_FEATURE_ID)
                .setImplementationVersion(implementationVersion())
                .setReceiveLimits(createLimits())
                .setStateReportPolicy(reportPolicy)
                .setEntityRefPolicy(HandshakeV0.EntityRefPolicy.newInstance()
                        .setPlayerIdMode(HandshakeV0.PlayerIdMode.PLAYER_ID_MODE_FORBIDDEN));
        addAnimationNames(result::addRequiredDefaultAnimations,
                defaultAnimationNames());
        return result;
    }

    public static void sendServerHello(ServerPlayer player) {
        var hello = createServerHello();
        GameServerPlayerStateSession.offer(player.connection.connection,
                ProtocolPolicies.fromServerHello(hello));
        NetworkHandler.sendToClientPlayer(hello, player);
    }

    public static HandshakeV0.ClientHello createClientHello() {
        var result = HandshakeV0.ClientHello.newInstance()
                .setProtocolVersion(ProtocolVersion.CURRENT)
                .addSupportedFeatureIds(ProtocolVersion.ASSET_TRANSFER_RELEASE_FEATURE_ID)
                .setImplementationVersion(implementationVersion())
                .setReceiveLimits(createLimits());
        addAnimationNames(result::addAvailableDefaultAnimations,
                defaultAnimationNames());
        return result;
    }

    public static void handleServerHello(HandshakeV0.ServerHello message,
                                         Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        if (!message.getProtocolVersion().equals(ProtocolVersion.CURRENT)) {
            context.getNetworkManager().disconnect(Component.literal(
                    "YSM protocol version is incompatible: expected "
                            + ProtocolVersion.CURRENT));
            context.setPacketHandled(true);
            return;
        }
        final PeerProtocolProfile profile;
        try {
            profile = ProtocolProfiles.fromServerHello(message);
        } catch (IllegalArgumentException error) {
            context.getNetworkManager().disconnect(Component.literal(
                    "YSM server sent invalid protocol limits"));
            context.setPacketHandled(true);
            return;
        }
        if (!ProtocolVersion.supportsRequiredFeatures(profile)) {
            context.getNetworkManager().disconnect(Component.literal(
                    "YSM server does not support required asset transfer release credits"));
            context.setPacketHandled(true);
            return;
        }
        final Set<DefaultAnimationKey> missing;
        try {
            missing = DefaultAnimationNegotiation.missing(
                    message.getRequiredDefaultAnimations(), defaultAnimationNames());
        } catch (IllegalArgumentException error) {
            context.getNetworkManager().disconnect(Component.literal(
                    "YSM server sent an invalid default animation contract"));
            context.setPacketHandled(true);
            return;
        }
        if (!missing.isEmpty()) {
            context.getNetworkManager().disconnect(Component.literal(
                    "YSM default animation contract is incomplete; missing "
                            + summarize(missing)));
            context.setPacketHandled(true);
            return;
        }
        final com.elfmcys.ysm.network.protocol.NegotiatedSessionPolicy policy;
        try {
            policy = ProtocolPolicies.fromServerHello(message);
        } catch (IllegalArgumentException error) {
            context.setPacketHandled(true);
            return;
        }
        if (policy.playerIdMode() != PlayerIdTransmissionMode.FORBIDDEN) {
            context.setPacketHandled(true);
            return;
        }
        if (!ClientSessionRuntime.acceptGameServer(profile, policy)) {
            context.setPacketHandled(true);
            return;
        }
        NetworkHandler.setPeerProfile(context.getNetworkManager(), profile);
        NetworkHandler.setChannelVersion(context.getNetworkManager(), ProtocolVersion.TRANSPORT_VERSION);
        warnVersionDifference(message.getImplementationVersion(), false);
        context.enqueueWork(() -> ClientModelService.instance().serverHandshake());
        NetworkHandler.sendToServer(createClientHello());
        context.setPacketHandled(true);
    }

    public static void handleClientHello(HandshakeV0.ClientHello message,
                                         Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        var player = context.getSender();
        if (player == null) {
            context.setPacketHandled(true);
            return;
        }
        if (!message.getProtocolVersion().equals(ProtocolVersion.CURRENT)) {
            player.connection.disconnect(Component.literal(
                    "YSM protocol version is incompatible: expected "
                            + ProtocolVersion.CURRENT));
            context.setPacketHandled(true);
            return;
        }
        final PeerProtocolProfile profile;
        try {
            profile = ProtocolProfiles.fromClientHello(message);
        } catch (IllegalArgumentException error) {
            player.connection.disconnect(Component.literal(
                    "YSM client sent invalid protocol limits"));
            context.setPacketHandled(true);
            return;
        }
        if (!ProtocolVersion.supportsRequiredFeatures(profile)) {
            player.connection.disconnect(Component.literal(
                    "YSM client does not support required asset transfer release credits"));
            context.setPacketHandled(true);
            return;
        }
        final Set<DefaultAnimationKey> missing;
        try {
            missing = DefaultAnimationNegotiation.missing(
                    defaultAnimationNames(), message.getAvailableDefaultAnimations());
        } catch (IllegalArgumentException error) {
            player.connection.disconnect(Component.literal(
                    "YSM client sent an invalid default animation contract"));
            context.setPacketHandled(true);
            return;
        }
        if (!missing.isEmpty()) {
            player.connection.disconnect(Component.literal(
                    "YSM client is missing required default animations: "
                            + summarize(missing)));
            context.setPacketHandled(true);
            return;
        }
        var stateSession = GameServerPlayerStateSession.offered(context.getNetworkManager()).orElse(null);
        if (stateSession == null) {
            context.setPacketHandled(true);
            return;
        }
        warnVersionDifference(message.getImplementationVersion(), true);
        try {
            if (!NetworkHandler.setPeerProfile(context.getNetworkManager(), profile)
                    || !NetworkHandler.setChannelVersion(context.getNetworkManager(), ProtocolVersion.TRANSPORT_VERSION)) {
                context.setPacketHandled(true);
                return;
            }
        } catch (IllegalArgumentException error) {
            context.setPacketHandled(true);
            return;
        }
        if (!stateSession.activate()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> {
            player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(capability -> {
                capability.setMandatory(false);
                capability.applyClientAnimation("");
            });
            PlayerStateHandler.sendAuthoritativeFull(player, false);
            player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(capability ->
                    NetworkHandler.sendToClientPlayer(ControlHandler.authorizedModels(capability.getAuthModels(), 1), player));
            player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP).ifPresent(capability ->
                    NetworkHandler.sendToClientPlayer(ControlHandler.starredModels(capability.getStarModels(), 1), player));
            ServerModelService.instance().sendCatalog(player);
        });
        context.setPacketHandled(true);
    }

    private static HandshakeV0.ProtocolLimits createLimits() {
        return HandshakeV0.ProtocolLimits.newInstance()
                .setMaxMessageBytes(ProtocolLimits.MAX_MESSAGE_BYTES)
                .setMaxFragmentBytes(ProtocolLimits.MAX_FRAGMENT_BYTES)
                .setMaxInFlightTransfers(ProtocolLimits.MAX_IN_FLIGHT_TRANSFERS)
                .setMaxEncodedAssetBytes(ProtocolLimits.MAX_ENCODED_ASSET_BYTES)
                .setMaxDecodedAssetBytes(ProtocolLimits.MAX_DECODED_ASSET_BYTES);
    }

    private static void addAnimationNames(
            java.util.function.Consumer<HandshakeV0.AnimationName> output,
            Set<DefaultAnimationKey> names) {
        names.stream().sorted().forEach(key -> output.accept(
                HandshakeV0.AnimationName.newInstance()
                        .setDomain(key.domain()).setName(key.name())));
    }

    private static Set<DefaultAnimationKey> defaultAnimationNames() {
        return ModelRuntime.system().builtinContract().defaultAnimationNames();
    }

    private static String summarize(Set<DefaultAnimationKey> missing) {
        var values = missing.stream().sorted().limit(5).map(key ->
                key.domain() + ":" + key.name()).toList();
        return values + (missing.size() > values.size()
                ? " and " + (missing.size() - values.size()) + " more" : "");
    }

    private static String implementationVersion() {
        return ModList.get().getModFileById(YesSteveModel.MOD_ID).versionString();
    }

    private static void warnVersionDifference(String peerVersion, boolean clientIsPeer) {
        var local = implementationVersion();
        if (local.equals(peerVersion)) {
            return;
        }
        YesSteveModel.LOGGER.warn(
                "YSM version difference accepted after animation-name coverage check: local={}, peer={}",
                local, peerVersion);
        if (!clientIsPeer) {
            Minecraft.getInstance().execute(() -> {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    player.displayClientMessage(Component.literal(
                            "YSM version differs from the server (client " + local
                                    + ", server " + peerVersion
                                    + "); animation compatibility check passed."), false);
                }
            });
        }
    }
}
