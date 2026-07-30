package com.elfmcys.ysm.network.session;

import com.elfmcys.ysm.model.source.SourceKind;

import java.util.Objects;

/** Keeps model discovery independent from player-state session selection. */
public final class SessionModelSourcePolicy {
    private SessionModelSourcePolicy() {
    }

    public static boolean isEnabled(SourceKind kind, ActiveSessionMode sessionMode) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(sessionMode, "sessionMode");
        return switch (kind) {
            case BUILTIN, LOCAL, EXTERNAL -> true;
            case GAME_SERVER -> sessionMode == ActiveSessionMode.GAME_SERVER;
            case BACKEND -> sessionMode == ActiveSessionMode.BACKEND;
        };
    }
}
