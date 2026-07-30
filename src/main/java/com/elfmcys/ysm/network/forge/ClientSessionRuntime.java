package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.client.model.ClientModelService;
import com.elfmcys.ysm.config.ClientConfig;
import com.elfmcys.ysm.network.session.ActiveSessionMode;
import com.elfmcys.ysm.network.session.SessionCoordinator;
import com.elfmcys.ysm.network.session.SessionMode;
import com.elfmcys.ysm.network.session.SessionSnapshot;
import com.elfmcys.ysm.network.session.SyncSession;
import com.elfmcys.ysm.network.protocol.NegotiatedSessionPolicy;
import com.elfmcys.ysm.network.protocol.PeerProtocolProfile;
import com.elfmcys.ysm.network.session.ProtocolTransport;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Forge lifecycle adapter for the transport-independent session coordinator. */
public final class ClientSessionRuntime {
    private static final SessionCoordinator COORDINATOR = new SessionCoordinator(
            () -> CompletableFuture.completedFuture(Optional.empty()),
            ClientSessionRuntime::sessionChanged);
    private static long connectionGeneration;

    private ClientSessionRuntime() {
    }

    public static synchronized long beginConnection() {
        connectionGeneration++;
        COORDINATOR.beginConnection(ClientConfig.NETWORK_SESSION_MODE.get());
        return connectionGeneration;
    }

    public static synchronized void completeGameServerDetection(long generation) {
        if (generation == connectionGeneration) {
            COORDINATOR.completeGameServerDetection();
        }
    }

    public static synchronized boolean acceptGameServer(PeerProtocolProfile profile,
                                                        NegotiatedSessionPolicy policy) {
        return COORDINATOR.acceptGameServer(new GameServerSyncSession(profile, policy));
    }

    public static synchronized SessionSnapshot snapshot() {
        return COORDINATOR.snapshot();
    }

    public static synchronized Optional<ProtocolTransport> transport() {
        return COORDINATOR.activeSession().flatMap(SyncSession::transport);
    }

    public static synchronized Optional<NegotiatedSessionPolicy> policy() {
        return COORDINATOR.activeSession().flatMap(SyncSession::policy);
    }

    public static void disconnect() {
        synchronized (ClientSessionRuntime.class) {
            connectionGeneration++;
            COORDINATOR.close();
        }
        ClientProtocolGateway.resetWorld();
        PlayerStateHandler.resetClientState();
        ClientModelService.instance().disconnect();
    }

    private static void sessionChanged(SessionSnapshot snapshot) {
        if (snapshot.requestedMode() == SessionMode.BACKEND
                && snapshot.activeMode().orElse(null) == ActiveSessionMode.LOCAL_ONLY) {
            YesSteveModel.LOGGER.warn("Backend session mode was requested, but no backend connector is configured; using Local Only");
        }
    }

    private static final class GameServerSyncSession implements SyncSession {
        private final ProtocolTransport transport;
        private final NegotiatedSessionPolicy policy;

        private GameServerSyncSession(PeerProtocolProfile profile, NegotiatedSessionPolicy policy) {
            this.transport = new ForgeClientProtocolTransport(profile);
            this.policy = policy;
        }

        @Override
        public ActiveSessionMode mode() {
            return ActiveSessionMode.GAME_SERVER;
        }

        @Override
        public Optional<ProtocolTransport> transport() {
            return Optional.of(transport);
        }

        @Override
        public Optional<NegotiatedSessionPolicy> policy() {
            return Optional.of(policy);
        }

        @Override
        public void close() {
            transport.close();
        }
    }
}
