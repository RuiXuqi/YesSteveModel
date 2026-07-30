package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.capability.ModelInfoCapability;
import com.elfmcys.ysm.capability.ModelInfoCapabilityProvider;
import com.elfmcys.ysm.capability.ModelInfoSyncAssembler;
import com.elfmcys.ysm.capability.ModelSelectionService;
import com.elfmcys.ysm.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.ysm.config.ServerConfig;
import com.elfmcys.ysm.geckolib3.core.molang.util.StringPool;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.network.protocol.ModelReferenceCodec;
import com.elfmcys.ysm.model.server.ServerModelService;
import com.elfmcys.ysm.network.NetworkHandler;
import com.elfmcys.ysm.network.protocol.PlayerStateSection;
import com.elfmcys.ysm.network.protocol.PlayerStateValidator;
import com.elfmcys.ysm.proto.network.protocol.v0.CommonV0;
import com.elfmcys.ysm.proto.network.protocol.v0.PlayerStateV0;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.function.Supplier;

public final class PlayerStateHandler {
    private static final int MAX_ROAMING_VARIABLES = 64;
    private static final int MAX_VARIABLE_NAME_BYTES = 32;
    private static final int MAX_ANIMATION_ID_BYTES = 256;
    private static final Int2LongOpenHashMap CLIENT_REVISIONS = new Int2LongOpenHashMap();

    private PlayerStateHandler() {
    }

    public static PlayerStateV0.PlayerStateUpdate newFull(ServerPlayer player,
                                                           ModelInfoCapability capability) {
        return PlayerStateV0.PlayerStateUpdate.newInstance()
                .setSubject(serverEntityRef(player.getId()))
                .setRevision(capability.nextStateRevision())
                .setMode(CommonV0.StateWriteMode.STATE_WRITE_MODE_FULL);
    }

    public static PlayerStateV0.PlayerStateUpdate newDelta(ServerPlayer player) {
        var update = PlayerStateV0.PlayerStateUpdate.newInstance()
                .setSubject(serverEntityRef(player.getId()))
                .setMode(CommonV0.StateWriteMode.STATE_WRITE_MODE_DELTA);
        player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP)
                .ifPresent(capability -> update.setRevision(capability.nextStateRevision()));
        return update;
    }

    private static PlayerStateV0.PlayerStateUpdate newDelta(ServerPlayer player,
                                                             ModelInfoCapability capability) {
        return PlayerStateV0.PlayerStateUpdate.newInstance()
                .setSubject(serverEntityRef(player.getId()))
                .setRevision(capability.nextStateRevision())
                .setMode(CommonV0.StateWriteMode.STATE_WRITE_MODE_DELTA);
    }

    public static void broadcast(ServerPlayer player, PlayerStateV0.PlayerStateUpdate update) {
        if (update.getRevision() != 0) {
            NetworkHandler.broadcastToVisiblePlayersAndSelf(update, player);
        }
    }

    public static void handleReport(PlayerStateV0.PlayerStateReport report,
                                    Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        var sender = context.getSender();
        if (sender == null) {
            context.setPacketHandled(true);
            return;
        }
        var stateSession = GameServerPlayerStateSession.active(context.getNetworkManager()).orElse(null);
        if (stateSession == null) {
            context.setPacketHandled(true);
            return;
        }
        if (!PlayerStateValidator.validReport(report)
                || !validServerReportHeader(report, sender)) {
            stateSession.warnRejected(sender, "invalid report structure or subject");
            context.setPacketHandled(true);
            return;
        }
        if (!stateSession.acceptsProjection(report)) {
            stateSession.warnRejected(sender, "report contains sections outside the negotiated policy");
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> sender.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP)
                .ifPresent(capability -> applyReport(sender, capability, stateSession, report)));
        context.setPacketHandled(true);
    }

    public static void handleUpdate(PlayerStateV0.PlayerStateUpdate update,
                                    Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        if (!PlayerStateValidator.validUpdate(update)) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> applyClientUpdate(update));
        context.setPacketHandled(true);
    }

    public static void removeClientEntity(int entityId) {
        CLIENT_REVISIONS.remove(entityId);
    }

    public static void resetClientState() {
        CLIENT_REVISIONS.clear();
    }

    public static boolean sendAuthoritativeFull(ServerPlayer player, boolean broadcast) {
        var capability = player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP)
                .resolve().orElse(null);
        if (capability == null) {
            return false;
        }
        if (broadcast) {
            capability.getPropertiesTracker().tick(player, false,
                    ServerConfig.LOW_BANDWIDTH_USAGE.get());
        }
        var update = buildAuthoritativeFull(player, capability).orElse(null);
        if (update == null) {
            capability.markDirty();
            return false;
        }
        if (broadcast) {
            capability.clearDirty();
            NetworkHandler.broadcastToVisiblePlayersAndSelf(update, player);
        } else {
            NetworkHandler.sendToClientPlayer(update, player);
        }
        return true;
    }

    private static boolean validServerReportHeader(PlayerStateV0.PlayerStateReport report,
                                                   ServerPlayer sender) {
        return report.hasSubject()
                && !report.getSubject().hasPlayerId()
                && report.getSubject().getEntityId() == sender.getId()
                && report.getSequence() != 0;
    }

    private static void applyReport(ServerPlayer sender, ModelInfoCapability capability,
                                    GameServerPlayerStateSession stateSession,
                                    PlayerStateV0.PlayerStateReport report) {
        var full = report.getMode() == CommonV0.StateWriteMode.STATE_WRITE_MODE_FULL;
        if (full) {
            var snapshot = ServerModelService.current().flatMap(ServerModelService::snapshot).orElse(null);
            if (snapshot == null || ModelSelectionService.resolve(capability, snapshot).isEmpty()) {
                stateSession.warnRejected(sender, "authoritative model state is temporarily unavailable");
                return;
            }
        }
        if (report.hasAnimation() && report.getAnimation().hasAnimationId()
                && !isAnimationAllowed(capability, report.getAnimation().getAnimationId())) {
            stateSession.warnRejected(sender, "animation is not available on the selected model");
            return;
        }
        if (report.hasRoaming() && !validRoaming(capability, report.getRoaming())) {
            stateSession.warnRejected(sender, "roaming state does not match the selected model");
            return;
        }
        var sections = stateSession.policy().stateReportPolicy().requestedSections();
        if (full && sections.contains(PlayerStateSection.ROAMING)
                && !report.hasRoaming() && capability.getModelHash() == null) {
            stateSession.warnRejected(sender, "full roaming reset has no authoritative model");
            return;
        }
        if (!stateSession.canAcceptSequence(report.getSequence(), full)) {
            stateSession.warnRejected(sender, "report sequence is stale or the first report is not FULL");
            return;
        }

        PlayerStateV0.AnimationState animation = null;
        if (sections.contains(PlayerStateSection.ANIMATION)) {
            if (report.hasAnimation()) {
                animation = report.getAnimation().hasStopped()
                        ? PlayerStateV0.AnimationState.newInstance().setStopped(true)
                        : PlayerStateV0.AnimationState.newInstance()
                        .setAnimationId(report.getAnimation().getAnimationId());
            } else if (full) {
                animation = PlayerStateV0.AnimationState.newInstance().setStopped(true);
            }
        }

        Object2FloatOpenHashMap<String> roamingVariables = null;
        var roamingKey = 0;
        if (sections.contains(PlayerStateSection.ROAMING) && (report.hasRoaming() || full)) {
            roamingKey = report.hasRoaming() ? report.getRoaming().getModelKey()
                    : capability.getModelHash().roamingHash();
            roamingVariables = new Object2FloatOpenHashMap<>(
                    report.hasRoaming() ? report.getRoaming().getVariables().length() : 0);
        }
        if (report.hasRoaming()) {
            if (roamingVariables == null) {
                stateSession.warnRejected(sender, "roaming state is outside the negotiated projection");
                return;
            }
            for (var variable : report.getRoaming().getVariables()) {
                roamingVariables.put(variable.getName(), variable.getValue());
            }
        }

        if (animation != null) {
            capability.applyClientAnimation(animation.hasStopped() ? "" : animation.getAnimationId());
        }
        if (roamingVariables != null) {
            capability.applyClientRoaming(roamingKey, roamingVariables, full);
        }

        final PlayerStateV0.PlayerStateUpdate update;
        if (full) {
            capability.getPropertiesTracker().tick(sender, false,
                    !sections.contains(PlayerStateSection.ROAMING));
            update = buildAuthoritativeFull(sender, capability).orElse(null);
            if (update == null) {
                capability.markDirty();
                stateSession.warnRejected(sender, "authoritative FULL state is temporarily unavailable");
                return;
            }
        } else {
            update = newDelta(sender, capability);
            if (animation != null) {
                update.setAnimation(animation);
            }
            if (roamingVariables != null) {
                var roaming = PlayerStateV0.RoamingState.newInstance().setModelKey(roamingKey);
                roamingVariables.object2FloatEntrySet().fastForEach(entry -> roaming.addVariables(
                        CommonV0.MolangVariable.newInstance()
                                .setName(entry.getKey()).setValue(entry.getFloatValue())));
                update.setRoaming(roaming);
            }
        }
        stateSession.commitSequence(report.getSequence(), full);
        if (full) {
            capability.clearDirty();
        }
        broadcast(sender, update);
    }

    private static boolean isAnimationAllowed(ModelInfoCapability capability, String animationId) {
        if (animationId.isBlank() || animationId.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                > MAX_ANIMATION_ID_BYTES || capability.getModelHash() == null) {
            return false;
        }
        return ServerModelService.current().flatMap(ServerModelService::snapshot)
                .flatMap(snapshot -> snapshot.find(capability.getModelHash()))
                .map(model -> {
                    var settings = model.view().getManifest().getInfo().getSettings();
                    if (settings.hasExtraAnimation()) {
                        for (var animation : settings.getExtraAnimation()) {
                            if (animationId.equals(animation.getKey())) return true;
                        }
                    }
                    if (settings.hasExtraAnimationClassify()) {
                        for (var classification : settings.getExtraAnimationClassify()) {
                            if (!classification.hasExtraAnimation()) {
                                continue;
                            }
                            for (var animation : classification.getExtraAnimation()) {
                                if (animationId.equals(animation.getKey())) return true;
                            }
                        }
                    }
                    return false;
                }).orElse(false);
    }

    private static boolean validRoaming(ModelInfoCapability capability, PlayerStateV0.RoamingState roaming) {
        if (capability.getModelHash() == null || capability.getModelHash().roamingHash() != roaming.getModelKey()
                || roaming.getVariables().length() > MAX_ROAMING_VARIABLES) {
            return false;
        }
        var names = new HashSet<String>();
        for (var variable : roaming.getVariables()) {
            if (!Float.isFinite(variable.getValue()) || variable.getName().isBlank()
                    || variable.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                    > MAX_VARIABLE_NAME_BYTES || !names.add(variable.getName())) {
                return false;
            }
        }
        return true;
    }

    private static void applyClientUpdate(PlayerStateV0.PlayerStateUpdate update) {
        var entityId = update.getSubject().getEntityId();
        var level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(entityId) instanceof Player player)) {
            return;
        }
        if (CLIENT_REVISIONS.containsKey(entityId)
                && Long.compareUnsigned(update.getRevision(), CLIENT_REVISIONS.get(entityId)) <= 0) {
            return;
        }
        player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(capability -> {
            var full = update.getMode() == CommonV0.StateWriteMode.STATE_WRITE_MODE_FULL;
            if (full) {
                capability.getStateTracker().reset();
            }
            if (update.hasModel()) {
                var model = update.getModel();
                ModelHash hash = ModelReferenceCodec.read(model.getModel());
                capability.updateModelAndTexture(hash, model.getTextureId());
                capability.setDisabled(model.getDisabled());
            }
            capability.getStateTracker().updateProtocolState(
                    update.hasGameplay() ? update.getGameplay() : null,
                    update.hasEffects() ? update.getEffects() : null,
                    full);
            if (update.hasAnimation()) {
                if (update.getAnimation().hasStopped()) {
                    capability.stopExtraAnimation();
                } else {
                    capability.playExtraAnimation(update.getAnimation().getAnimationId());
                }
            } else if (full) {
                capability.stopExtraAnimation();
            }
            if (update.hasRoaming()) {
                var values = new Int2FloatOpenHashMap(update.getRoaming().getVariables().length());
                for (var variable : update.getRoaming().getVariables()) {
                    values.put(StringPool.computeIfAbsent(variable.getName()), variable.getValue());
                }
                if (full) {
                    capability.resetRoamingVars(update.getRoaming().getModelKey(), values);
                } else {
                    capability.updateRemoteRoamingVars(update.getRoaming().getModelKey(), values);
                }
            }
            CLIENT_REVISIONS.put(entityId, update.getRevision());
            if (full && player == Minecraft.getInstance().player) {
                var modelHash = ModelReferenceCodec.read(update.getModel().getModel());
                ClientProtocolGateway.acceptAuthoritativeFull(modelHash,
                        update.hasRoaming() ? update.getRoaming().getModelKey() : null);
            }
        });
    }

    private static java.util.Optional<PlayerStateV0.PlayerStateUpdate> buildAuthoritativeFull(
            ServerPlayer player, ModelInfoCapability capability) {
        return ServerModelService.current().flatMap(ServerModelService::snapshot)
                .flatMap(snapshot -> ModelInfoSyncAssembler.build(
                        player, capability, snapshot));
    }

    private static CommonV0.EntityRef serverEntityRef(int entityId) {
        return CommonV0.EntityRef.newInstance().setEntityId(entityId);
    }
}
