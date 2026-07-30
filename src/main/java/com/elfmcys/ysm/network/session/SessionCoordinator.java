package com.elfmcys.ysm.network.session;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Selects one remote player-state authority for a game connection.
 * Model sources are deliberately outside this state machine.
 */
public final class SessionCoordinator implements AutoCloseable {
    private static final SyncSession LOCAL_ONLY_SESSION = new SyncSession() {
        @Override
        public ActiveSessionMode mode() {
            return ActiveSessionMode.LOCAL_ONLY;
        }

        @Override
        public java.util.Optional<ProtocolTransport> transport() {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<com.elfmcys.ysm.network.protocol.NegotiatedSessionPolicy> policy() {
            return java.util.Optional.empty();
        }

        @Override
        public void close() {
        }
    };

    private final BackendSessionConnector backendConnector;
    private final Consumer<SessionSnapshot> listener;
    private SessionMode requestedMode = SessionMode.LOCAL_ONLY;
    private SyncSession activeSession = LOCAL_ONLY_SESSION;
    private boolean awaitingGameServerDetection;
    private long generation;

    public SessionCoordinator(BackendSessionConnector backendConnector,
                              Consumer<SessionSnapshot> listener) {
        this.backendConnector = Objects.requireNonNull(backendConnector, "backendConnector");
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    public void beginConnection(SessionMode mode) {
        Objects.requireNonNull(mode, "mode");
        long currentGeneration;
        synchronized (this) {
            generation++;
            currentGeneration = generation;
            closeActiveSession();
            requestedMode = mode;
            awaitingGameServerDetection = mode == SessionMode.AUTO;
            if (mode == SessionMode.LOCAL_ONLY) {
                activeSession = LOCAL_ONLY_SESSION;
            }
            publish();
        }
        if (mode == SessionMode.BACKEND) {
            connectBackend(currentGeneration);
        }
    }

    /** Accepts the game-server session only while AUTO detection is still open. */
    public synchronized boolean acceptGameServer(SyncSession session) {
        Objects.requireNonNull(session, "session");
        if (session.mode() != ActiveSessionMode.GAME_SERVER) {
            throw new IllegalArgumentException("Expected a game-server session");
        }
        if (requestedMode != SessionMode.AUTO || !awaitingGameServerDetection || activeSession != null) {
            session.close();
            return false;
        }
        awaitingGameServerDetection = false;
        activeSession = session;
        publish();
        return true;
    }

    /** Finishes AUTO probing and falls back to Backend, then Local Only. */
    public void completeGameServerDetection() {
        long currentGeneration;
        synchronized (this) {
            if (requestedMode != SessionMode.AUTO || !awaitingGameServerDetection || activeSession != null) {
                return;
            }
            awaitingGameServerDetection = false;
            currentGeneration = generation;
            publish();
        }
        connectBackend(currentGeneration);
    }

    /** A lost game-server authority never changes to Backend during the same game connection. */
    public synchronized void gameServerDisconnected() {
        if (activeSession == null || activeSession.mode() != ActiveSessionMode.GAME_SERVER) {
            return;
        }
        closeActiveSession();
        activeSession = LOCAL_ONLY_SESSION;
        publish();
    }

    public synchronized SessionSnapshot snapshot() {
        return createSnapshot();
    }

    public synchronized Optional<SyncSession> activeSession() {
        return Optional.ofNullable(activeSession);
    }

    @Override
    public synchronized void close() {
        generation++;
        awaitingGameServerDetection = false;
        closeActiveSession();
        requestedMode = SessionMode.LOCAL_ONLY;
        activeSession = LOCAL_ONLY_SESSION;
        publish();
    }

    private void connectBackend(long expectedGeneration) {
        CompletionStage<Optional<SyncSession>> attempt;
        try {
            attempt = Objects.requireNonNull(backendConnector.connect(), "backend connection attempt");
        } catch (RuntimeException error) {
            finishBackendAttempt(expectedGeneration, Optional.empty());
            return;
        }
        attempt.whenComplete((session, error) -> finishBackendAttempt(
                expectedGeneration, error == null && session != null ? session : Optional.empty()));
    }

    private synchronized void finishBackendAttempt(long expectedGeneration, Optional<SyncSession> result) {
        if (expectedGeneration != generation || activeSession != null
                || (requestedMode != SessionMode.AUTO && requestedMode != SessionMode.BACKEND)) {
            result.ifPresent(SyncSession::close);
            return;
        }
        var session = result.orElse(null);
        if (session != null && session.mode() != ActiveSessionMode.BACKEND) {
            session.close();
            session = null;
        }
        activeSession = session != null ? session : LOCAL_ONLY_SESSION;
        publish();
    }

    private void closeActiveSession() {
        if (activeSession != null) {
            activeSession.close();
            activeSession = null;
        }
    }

    private SessionSnapshot createSnapshot() {
        return new SessionSnapshot(requestedMode,
                Optional.ofNullable(activeSession).map(SyncSession::mode),
                awaitingGameServerDetection);
    }

    private void publish() {
        listener.accept(createSnapshot());
    }
}
