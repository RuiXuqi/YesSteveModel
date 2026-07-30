package com.elfmcys.ysm.network.session;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionCoordinatorTest {
    @Test
    void autoPrefersGameServerAndNeverStartsBackend() {
        var backendAttempts = new int[1];
        var coordinator = new SessionCoordinator(() -> {
            backendAttempts[0]++;
            return CompletableFuture.completedFuture(Optional.empty());
        }, ignored -> {
        });

        coordinator.beginConnection(SessionMode.AUTO);
        assertTrue(coordinator.acceptGameServer(new TestSession(ActiveSessionMode.GAME_SERVER)));
        coordinator.completeGameServerDetection();

        assertEquals(ActiveSessionMode.GAME_SERVER, coordinator.snapshot().activeMode().orElseThrow());
        assertEquals(0, backendAttempts[0]);
    }

    @Test
    void autoFallsBackFromBackendToLocalOnly() {
        var coordinator = new SessionCoordinator(
                () -> CompletableFuture.completedFuture(Optional.empty()), ignored -> {
        });

        coordinator.beginConnection(SessionMode.AUTO);
        coordinator.completeGameServerDetection();

        assertEquals(ActiveSessionMode.LOCAL_ONLY, coordinator.snapshot().activeMode().orElseThrow());
    }

    @Test
    void forcedModesRejectGameServer() {
        for (var mode : new SessionMode[]{SessionMode.BACKEND, SessionMode.LOCAL_ONLY}) {
            var coordinator = new SessionCoordinator(
                    () -> CompletableFuture.completedFuture(Optional.empty()), ignored -> {
            });
            var offered = new TestSession(ActiveSessionMode.GAME_SERVER);

            coordinator.beginConnection(mode);

            assertFalse(coordinator.acceptGameServer(offered));
            assertTrue(offered.closed);
            assertEquals(ActiveSessionMode.LOCAL_ONLY, coordinator.snapshot().activeMode().orElseThrow());
        }
    }

    @Test
    void gameServerLossDoesNotFailOverToBackend() {
        var backendAttempts = new int[1];
        var coordinator = new SessionCoordinator(() -> {
            backendAttempts[0]++;
            return CompletableFuture.completedFuture(Optional.of(new TestSession(ActiveSessionMode.BACKEND)));
        }, ignored -> {
        });
        coordinator.beginConnection(SessionMode.AUTO);
        coordinator.acceptGameServer(new TestSession(ActiveSessionMode.GAME_SERVER));

        coordinator.gameServerDisconnected();

        assertEquals(ActiveSessionMode.LOCAL_ONLY, coordinator.snapshot().activeMode().orElseThrow());
        assertEquals(0, backendAttempts[0]);
    }

    @Test
    void staleBackendResultCannotReplaceNewConnection() {
        var pending = new CompletableFuture<Optional<SyncSession>>();
        var snapshots = new ArrayList<SessionSnapshot>();
        var coordinator = new SessionCoordinator(() -> pending, snapshots::add);
        coordinator.beginConnection(SessionMode.BACKEND);
        coordinator.beginConnection(SessionMode.LOCAL_ONLY);
        var stale = new TestSession(ActiveSessionMode.BACKEND);

        pending.complete(Optional.of(stale));

        assertTrue(stale.closed);
        assertEquals(ActiveSessionMode.LOCAL_ONLY, coordinator.snapshot().activeMode().orElseThrow());
    }

    private static final class TestSession implements SyncSession {
        private final ActiveSessionMode mode;
        private boolean closed;

        private TestSession(ActiveSessionMode mode) {
            this.mode = mode;
        }

        @Override
        public ActiveSessionMode mode() {
            return mode;
        }

        @Override
        public Optional<ProtocolTransport> transport() {
            return Optional.empty();
        }

        @Override
        public Optional<com.elfmcys.ysm.network.protocol.NegotiatedSessionPolicy> policy() {
            return Optional.empty();
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
