package com.elfmcys.ysm.network.protocol;

import com.elfmcys.ysm.proto.network.protocol.v0.CommonV0;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public final class EntityRefEncoder {
    private final Map<Integer, UUID> observedPlayers = new HashMap<>();
    private final Map<Integer, PlayerId> advertisedPlayers = new HashMap<>();
    private final Function<UUID, PlayerId> playerIdDerivation;
    private PlayerIdTransmissionMode mode = PlayerIdTransmissionMode.FORBIDDEN;

    public EntityRefEncoder(Function<UUID, PlayerId> playerIdDerivation) {
        this.playerIdDerivation = Objects.requireNonNull(playerIdDerivation, "playerIdDerivation");
    }

    public synchronized void setMode(PlayerIdTransmissionMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
        advertisedPlayers.clear();
    }

    public synchronized void observePlayer(int entityId, UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        var previous = observedPlayers.put(entityId, uuid);
        if (!uuid.equals(previous)) {
            advertisedPlayers.remove(entityId);
        }
    }

    public synchronized void removeEntity(int entityId) {
        observedPlayers.remove(entityId);
        advertisedPlayers.remove(entityId);
    }

    public synchronized void resetWorld() {
        observedPlayers.clear();
        advertisedPlayers.clear();
    }

    public synchronized void resetSession() {
        advertisedPlayers.clear();
    }

    public synchronized EncodedEntityRef encodePlayer(int entityId) {
        var uuid = observedPlayers.get(entityId);
        if (uuid == null) {
            throw new IllegalStateException("Player entity is not present in the observed identity map: " + entityId);
        }
        var value = CommonV0.EntityRef.newInstance().setEntityId(entityId);
        if (mode == PlayerIdTransmissionMode.FORBIDDEN) {
            return new EncodedEntityRef(value, () -> { });
        }
        var playerId = playerIdDerivation.apply(uuid);
        if (playerId.equals(advertisedPlayers.get(entityId))) {
            return new EncodedEntityRef(value, () -> { });
        }
        value.setPlayerId(playerId.bytes());
        return new EncodedEntityRef(value, () -> markAdvertised(entityId, uuid, playerId));
    }

    public static EncodedEntityRef encodeNonPlayer(int entityId) {
        return new EncodedEntityRef(CommonV0.EntityRef.newInstance().setEntityId(entityId), () -> { });
    }

    private synchronized void markAdvertised(int entityId, UUID expectedUuid, PlayerId playerId) {
        if (expectedUuid.equals(observedPlayers.get(entityId))) {
            advertisedPlayers.put(entityId, playerId);
        }
    }

    public record EncodedEntityRef(CommonV0.EntityRef value, Runnable sentCommit) {
        public EncodedEntityRef {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(sentCommit, "sentCommit");
        }
    }
}
