package com.elfmcys.ysm.network.session;

import java.util.Optional;

public record SessionSnapshot(SessionMode requestedMode,
                              Optional<ActiveSessionMode> activeMode,
                              boolean awaitingGameServerDetection) {
}
