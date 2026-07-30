package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.network.protocol.NegotiatedSessionPolicy;
import com.elfmcys.ysm.network.protocol.PlayerStateReportCursor;
import com.elfmcys.ysm.proto.network.protocol.v0.PlayerStateV0;
import com.elfmcys.ysm.util.TokenBucket;
import io.netty.util.AttributeKey;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;

/** Connection-owned server state for the negotiated player-state report stream. */
final class GameServerPlayerStateSession {
    private static final AttributeKey<GameServerPlayerStateSession> ATTRIBUTE =
            AttributeKey.valueOf(YesSteveModel.MOD_ID + "_player_state_session");

    private final NegotiatedSessionPolicy policy;
    private final PlayerStateReportCursor cursor = new PlayerStateReportCursor();
    private final TokenBucket rejectionWarnings = new TokenBucket(3, 0.2F);
    private boolean active;

    private GameServerPlayerStateSession(NegotiatedSessionPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    static void offer(Connection connection, NegotiatedSessionPolicy policy) {
        Objects.requireNonNull(connection, "connection");
        connection.channel().attr(ATTRIBUTE).set(new GameServerPlayerStateSession(policy));
    }

    static Optional<GameServerPlayerStateSession> offered(Connection connection) {
        return find(connection);
    }

    static Optional<GameServerPlayerStateSession> active(Connection connection) {
        return find(connection).filter(GameServerPlayerStateSession::isActive);
    }

    NegotiatedSessionPolicy policy() {
        return policy;
    }

    boolean acceptsProjection(PlayerStateV0.PlayerStateReport report) {
        return policy.stateReportPolicy().acceptsProjection(report);
    }

    boolean canAcceptSequence(long sequence, boolean full) {
        return cursor.canAccept(sequence, full);
    }

    void commitSequence(long sequence, boolean full) {
        cursor.commit(sequence, full);
    }

    synchronized void warnRejected(ServerPlayer player, String reason) {
        if (rejectionWarnings.request()) {
            YesSteveModel.LOGGER.warn("Rejected player-state report from {}: {}",
                    player.getScoreboardName(), reason);
        }
    }

    synchronized boolean activate() {
        if (active) {
            return false;
        }
        active = true;
        return true;
    }

    private synchronized boolean isActive() {
        return active;
    }

    private static Optional<GameServerPlayerStateSession> find(Connection connection) {
        return connection == null || connection.channel() == null
                ? Optional.empty() : Optional.ofNullable(connection.channel().attr(ATTRIBUTE).get());
    }
}
