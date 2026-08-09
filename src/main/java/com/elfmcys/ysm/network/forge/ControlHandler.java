package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.capability.AuthModelsCapabilityProvider;
import com.elfmcys.ysm.capability.ModelInfoCapabilityProvider;
import com.elfmcys.ysm.capability.ModelSelectionService;
import com.elfmcys.ysm.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.ysm.capability.StarModelsCapabilityProvider;
import com.elfmcys.ysm.config.ServerConfig;
import com.elfmcys.ysm.client.animation.molang.CustomMolangParser;
import com.elfmcys.ysm.client.compat.touhoulittlemaid.TlmCommonCompat;
import com.elfmcys.ysm.event.CapabilityEvent;
import com.elfmcys.ysm.geckolib3.core.molang.value.IValue;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.network.protocol.ModelReferenceCodec;
import com.elfmcys.ysm.model.source.AccessPolicy;
import com.elfmcys.ysm.model.server.ServerModelService;
import com.elfmcys.ysm.molang.parser.ParseException;
import com.elfmcys.ysm.network.NetworkHandler;
import com.elfmcys.ysm.proto.network.protocol.v0.CommonV0;
import com.elfmcys.ysm.proto.network.protocol.v0.ControlV0;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public final class ControlHandler {
    private static final int MAX_MODEL_SET_SIZE = 16_384;
    private static final int MAX_MOLANG_TARGETS = 1024;
    private static final int MAX_MOLANG_ARGUMENTS = 16;
    private static final int MAX_EXPRESSION_LENGTH = 4096;

    private ControlHandler() {
    }

    public static ControlV0.AuthorizedModelsSnapshot authorizedModels(Set<Hash256> hashes, long revision) {
        var result = ControlV0.AuthorizedModelsSnapshot.newInstance().setRevision(revision);
        hashes.stream().sorted(Comparator.naturalOrder()).limit(MAX_MODEL_SET_SIZE)
                .forEach(hash -> result.addModelHashes(hash.bytes()));
        return result;
    }

    public static ControlV0.StarredModelsSnapshot starredModels(Set<Hash256> hashes, long revision) {
        var result = ControlV0.StarredModelsSnapshot.newInstance().setRevision(revision);
        hashes.stream().sorted(Comparator.naturalOrder()).limit(MAX_MODEL_SET_SIZE)
                .forEach(hash -> result.addModelHashes(hash.bytes()));
        return result;
    }

    public static void handleSelectModel(ControlV0.SelectModelRequest message,
                                         Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        var sender = context.getSender();
        if (sender != null && message.hasModel()
                && ModelReferenceCodec.valid(message.getModel())
                && message.getTextureId().length() <= 256) {
            var hash = ModelReferenceCodec.read(message.getModel());
            context.enqueueWork(() -> applyModelSelection(sender, hash,
                    message.getTextureId()));
        }
        context.setPacketHandled(true);
    }

    public static void handleAuthorizedModels(ControlV0.AuthorizedModelsSnapshot message,
                                              Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        var hashes = readHashSet(message.getModelHashes());
        if (hashes != null) context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP)
                    .ifPresent(capability -> capability.setAuthModels(hashes));
        });
        context.setPacketHandled(true);
    }

    public static void handleStarredModels(ControlV0.StarredModelsSnapshot message,
                                           Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        var hashes = readHashSet(message.getModelHashes());
        if (hashes != null) context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP)
                    .ifPresent(capability -> capability.setStarModels(hashes));
        });
        context.setPacketHandled(true);
    }

    public static void handleUpdateStar(ControlV0.UpdateStarredModelRequest message,
                                        Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        var sender = context.getSender();
        if (sender != null && message.getModelHash().length() == Hash256.SIZE
                && message.getOperation() != ControlV0.StarredModelOperation.STARRED_MODEL_OPERATION_UNSPECIFIED) {
            var hash = new Hash256(message.getModelHash().array(), 0, message.getModelHash().length());
            context.enqueueWork(() -> sender.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP)
                    .ifPresent(capability -> {
                        if (message.getOperation() == ControlV0.StarredModelOperation.STARRED_MODEL_OPERATION_ADD) {
                            capability.addModel(hash);
                        } else {
                            capability.removeModel(hash);
                        }
                    }));
        }
        context.setPacketHandled(true);
    }

    public static void handleEntityAnimation(ControlV0.EntityAnimationActionRequest message,
                                             Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        var sender = context.getSender();
        if (sender != null && validNonPlayerTarget(message.hasTarget() ? message.getTarget() : null)) {
            context.enqueueWork(() -> {
                var entity = sender.serverLevel().getEntity(message.getTarget().getEntityId());
                if (!TlmCommonCompat.canControlMaid(entity, sender)) return;
                if (message.hasStop() && message.getStop()) {
                    TlmCommonCompat.setRouletteAnim(entity, "", -1);
                } else if (message.hasPlay() && message.getPlay().getClassificationId().length() <= 128) {
                    TlmCommonCompat.setRouletteAnim(entity, message.getPlay().getClassificationId(),
                            message.getPlay().getAnimationIndex());
                }
            });
        }
        context.setPacketHandled(true);
    }

    public static void handleExecuteMolang(ControlV0.ExecuteMolangEvent message,
                                           Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        if (message.getTargets().length() <= MAX_MOLANG_TARGETS
                && message.getExpression().length() <= MAX_EXPRESSION_LENGTH) {
            context.enqueueWork(() -> executeMolang(message));
        }
        context.setPacketHandled(true);
    }

    public static void handleSubmitRoulette(ControlV0.SubmitRouletteExpressionRequest message,
                                            Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        var sender = context.getSender();
        if (sender != null && message.hasTarget() && !message.getTarget().hasPlayerId()
                && message.getExpression().length() <= MAX_EXPRESSION_LENGTH) {
            context.enqueueWork(() -> {
                var entity = sender.serverLevel().getEntity(message.getTarget().getEntityId());
                if (entity != sender && !TlmCommonCompat.canControlMaid(entity, sender)) return;
                var event = ControlV0.ExecuteMolangEvent.newInstance()
                        .addTargets(CommonV0.EntityRef.newInstance().setEntityId(entity.getId()))
                        .setExpression(message.getExpression());
                NetworkHandler.broadcastToVisiblePlayers(event, entity);
            });
        }
        context.setPacketHandled(true);
    }

    public static void handleEmitMolangSync(ControlV0.EmitMolangSync message,
                                            Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        var sender = context.getSender();
        if (sender != null && validArguments(message.getArguments())) {
            context.enqueueWork(() -> {
                var event = ControlV0.MolangSyncEvent.newInstance()
                        .setSubject(CommonV0.EntityRef.newInstance().setEntityId(sender.getId()));
                message.getArguments().forEach(event::addArguments);
                NetworkHandler.broadcastToVisiblePlayersAndSelf(event, sender);
            });
        }
        context.setPacketHandled(true);
    }

    public static void handleMolangSync(ControlV0.MolangSyncEvent message,
                                        Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        if (message.hasSubject() && !message.getSubject().hasPlayerId() && validArguments(message.getArguments())) {
            context.enqueueWork(() -> {
                var level = Minecraft.getInstance().level;
                var entity = level == null ? null : level.getEntity(message.getSubject().getEntityId());
                if (entity != null) entity.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(capability -> {
                    var args = new FloatArrayList(message.getArguments().length());
                    message.getArguments().forEach(args::add);
                    capability.molangSync(args);
                });
            });
        }
        context.setPacketHandled(true);
    }

    public static void handleSwingHand(ControlV0.SwingHandRequest message,
                                       Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        var sender = context.getSender();
        if (sender != null && message.getHand() != ControlV0.Hand.HAND_UNSPECIFIED) {
            context.enqueueWork(() -> swing(sender,
                    message.getHand() == ControlV0.Hand.HAND_MAIN ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND));
        }
        context.setPacketHandled(true);
    }

    private static void applyModelSelection(ServerPlayer sender, Hash256 hash, String textureId) {
        var modelCapability = sender.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP)
                .resolve().orElse(null);
        if (modelCapability == null) {
            return;
        }
        if (ServerConfig.CAN_SWITCH_MODEL.get()) {
            sender.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).resolve()
                    .ifPresent(authCapability -> ServerModelService.instance().snapshot().ifPresent(snapshot -> {
                        if (hash == null) {
                            ModelSelectionService.selectBuiltinDefault(
                                    modelCapability, snapshot, textureId);
                        } else {
                            var model = snapshot.find(hash);
                            if (model.isEmpty() || model.get().location().rootKind().accessPolicy()
                                    == AccessPolicy.SESSION_AUTHORIZED
                                    && !authCapability.containModel(hash)
                                    || !model.get().view().getPlayer().getTextureNames().contains(textureId)) {
                                ModelSelectionService.selectDefault(modelCapability, snapshot);
                            } else {
                                modelCapability.setModelAndTexture(hash, textureId);
                            }
                        }
                        modelCapability.applyClientAnimation("");
                    }));
        }
        if (!PlayerStateHandler.sendAuthoritativeFull(sender, true)) {
            modelCapability.markDirty();
        } else if (sender.getVehicle() != null && sender.getVehicle().getFirstPassenger() == sender) {
            CapabilityEvent.onVehicleSetModel(sender.getVehicle(), sender);
        }
    }

    static Set<Hash256> readHashSet(Iterable<us.hebi.quickbuf.RepeatedByte> values) {
        var result = new HashSet<Hash256>();
        var count = 0;
        for (var value : values) {
            if (++count > MAX_MODEL_SET_SIZE || value.length() != Hash256.SIZE) return null;
            if (!result.add(new Hash256(value.array(), 0, value.length()))) return null;
        }
        return result;
    }

    private static boolean validNonPlayerTarget(CommonV0.EntityRef target) {
        return target != null && !target.hasPlayerId();
    }

    private static boolean validArguments(us.hebi.quickbuf.RepeatedFloat values) {
        if (values.length() > MAX_MOLANG_ARGUMENTS) return false;
        for (var i = 0; i < values.length(); i++) if (!Float.isFinite(values.get(i))) return false;
        return true;
    }

    private static void executeMolang(ControlV0.ExecuteMolangEvent message) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (var target : message.getTargets()) {
            if (target.hasPlayerId()) continue;
            Entity entity = level.getEntity(target.getEntityId());
            if (entity instanceof Player player) {
                player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(capability -> {
                    try {
                        IValue value = CustomMolangParser.parseSingleExpressionUnsafe(message.getExpression());
                        capability.executeMolangExp(value, true, false, null);
                    } catch (ParseException error) {
                        YesSteveModel.LOGGER.error("Failed to execute molang " + message.getExpression(), error);
                    }
                });
            } else if (TlmCommonCompat.isMaid(entity)) {
                TlmCommonCompat.handleExecuteMolang(entity, message.getExpression());
            }
        }
    }

    private static void swing(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !stack.onEntitySwing(player)) {
            if (!player.swinging || player.swingTime >= currentSwingDuration(player) / 2 || player.swingTime < 0) {
                player.swingTime = -1;
                player.swinging = true;
                player.swingingArm = hand;
                if (player.level() instanceof ServerLevel level) {
                    var packet = new ClientboundAnimatePacket(player, hand == InteractionHand.MAIN_HAND ? 0 : 3);
                    ServerChunkCache chunks = level.getChunkSource();
                    chunks.broadcast(player, packet);
                }
            }
        }
    }

    private static int currentSwingDuration(LivingEntity entity) {
        if (MobEffectUtil.hasDigSpeed(entity)) return 6 - (1 + MobEffectUtil.getDigSpeedAmplification(entity));
        return entity.hasEffect(MobEffects.DIG_SLOWDOWN)
                ? 6 + (1 + entity.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) * 2 : 6;
    }
}
