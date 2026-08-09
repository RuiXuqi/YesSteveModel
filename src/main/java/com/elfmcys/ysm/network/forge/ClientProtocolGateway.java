package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.capability.PlayerAnimatableCapability;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.client.model.ClientModelService;
import com.elfmcys.ysm.network.protocol.ModelReferenceCodec;
import com.elfmcys.ysm.network.protocol.EntityRefEncoder;
import com.elfmcys.ysm.network.protocol.ProtocolMessages;
import com.elfmcys.ysm.proto.network.protocol.v0.ControlV0;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class ClientProtocolGateway {
    private static final EntityRefEncoder ENTITY_REFS = new EntityRefEncoder(PlayerIdDerivation::derive);
    private static final LocalPlayerStateReporter STATE_REPORTER = new LocalPlayerStateReporter(ENTITY_REFS);

    private ClientProtocolGateway() {
    }

    public static void tick(LocalPlayer player, PlayerAnimatableCapability capability) {
        STATE_REPORTER.tick(player, capability);
    }

    public static void observePlayer(int entityId, java.util.UUID uuid) {
        ENTITY_REFS.observePlayer(entityId, uuid);
    }

    public static void removeEntity(int entityId) {
        ENTITY_REFS.removeEntity(entityId);
    }

    public static void resetWorld() {
        ENTITY_REFS.resetWorld();
        STATE_REPORTER.resetSession();
    }

    public static void playSelfAnimation(String animationId) {
        STATE_REPORTER.setAnimation(animationId);
    }

    public static void stopSelfAnimation() {
        STATE_REPORTER.setAnimation("");
    }

    public static void acceptAuthoritativeFull(Hash256 modelHash, Integer roamingKey) {
        STATE_REPORTER.acceptAuthoritativeFull(modelHash, roamingKey);
    }

    public static void localPlayerCloned() {
        STATE_REPORTER.awaitAuthoritativeFull(true);
    }

    public static void playMaidAnimation(int entityId, int index, String classificationId) {
        var target = EntityRefEncoder.encodeNonPlayer(entityId);
        send(ProtocolMessages.ENTITY_ANIMATION_ACTION_REQUEST_ID,
                ControlV0.EntityAnimationActionRequest.newInstance()
                        .setTarget(target.value())
                        .setPlay(ControlV0.RouletteAnimationSelection.newInstance()
                                .setAnimationIndex(index)
                                .setClassificationId(classificationId)));
    }

    public static void stopMaidAnimation(int entityId) {
        send(ProtocolMessages.ENTITY_ANIMATION_ACTION_REQUEST_ID,
                ControlV0.EntityAnimationActionRequest.newInstance()
                        .setTarget(EntityRefEncoder.encodeNonPlayer(entityId).value())
                        .setStop(true));
    }

    public static void selectModel(Hash256 hash, String textureId) {
        var reference = com.elfmcys.ysm.proto.network.protocol.v0.CommonV0
                .ModelReference.newInstance();
        var defaultHash = ClientModelService.current()
                .map(service -> service.defaultRenderTarget().modelHash()).orElse(null);
        ModelReferenceCodec.write(reference, hash, defaultHash);
        var generation = STATE_REPORTER.awaitAuthoritativeFull(true);
        send(ProtocolMessages.SELECT_MODEL_REQUEST_ID, ControlV0.SelectModelRequest.newInstance()
                .setModel(reference).setTextureId(textureId)).whenComplete((ignored, error) -> {
                    if (error != null) {
                        STATE_REPORTER.resumeAfterRequestFailure(generation);
                    }
                });
    }

    public static void updateStar(Hash256 hash, boolean add) {
        send(ProtocolMessages.UPDATE_STARRED_MODEL_REQUEST_ID,
                ControlV0.UpdateStarredModelRequest.newInstance()
                        .setModelHash(hash.bytes())
                        .setOperation(add ? ControlV0.StarredModelOperation.STARRED_MODEL_OPERATION_ADD
                                : ControlV0.StarredModelOperation.STARRED_MODEL_OPERATION_REMOVE));
    }

    public static void submitRouletteExpression(Entity entity, String expression) {
        var target = entity instanceof Player ? ENTITY_REFS.encodePlayer(entity.getId())
                : EntityRefEncoder.encodeNonPlayer(entity.getId());
        send(ProtocolMessages.SUBMIT_ROULETTE_EXPRESSION_REQUEST_ID,
                ControlV0.SubmitRouletteExpressionRequest.newInstance()
                        .setTarget(target.value())
                        .setExpression(expression)).whenComplete((ignored, error) -> {
                            if (error == null) target.sentCommit().run();
                        });
    }

    public static void emitMolangSync(FloatList values) {
        var message = ControlV0.EmitMolangSync.newInstance();
        values.forEach((float value) -> message.addArguments(value));
        send(ProtocolMessages.EMIT_MOLANG_SYNC_ID, message);
    }

    public static void swingHand(InteractionHand hand) {
        send(ProtocolMessages.SWING_HAND_REQUEST_ID, ControlV0.SwingHandRequest.newInstance()
                .setHand(hand == InteractionHand.MAIN_HAND ? ControlV0.Hand.HAND_MAIN : ControlV0.Hand.HAND_OFF));
    }

    public static void reportRoamingChanges(int modelKey, Object2FloatMap<String> values) {
        // The reporter samples the authoritative local roaming structure on the next client tick.
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static CompletionStage<Void> send(int id, us.hebi.quickbuf.ProtoMessage message) {
        var transport = ClientSessionRuntime.transport().orElse(null);
        var spec = ProtocolMessages.REGISTRY.find(id).orElse(null);
        if (transport != null && spec != null) {
            return transport.send(spec, message);
        }
        return CompletableFuture.failedFuture(
                new IllegalStateException("No active protocol transport"));
    }
}
