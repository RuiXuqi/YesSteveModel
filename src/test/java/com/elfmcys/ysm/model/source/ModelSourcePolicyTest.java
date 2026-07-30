package com.elfmcys.ysm.model.source;

import com.elfmcys.ysm.network.session.ActiveSessionMode;
import com.elfmcys.ysm.network.session.SessionModelSourcePolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelSourcePolicyTest {
    @Test
    void localOnlyStillAllowsClientConfiguredExternalSources() {
        assertTrue(SessionModelSourcePolicy.isEnabled(SourceKind.BUILTIN, ActiveSessionMode.LOCAL_ONLY));
        assertTrue(SessionModelSourcePolicy.isEnabled(SourceKind.LOCAL, ActiveSessionMode.LOCAL_ONLY));
        assertTrue(SessionModelSourcePolicy.isEnabled(SourceKind.EXTERNAL, ActiveSessionMode.LOCAL_ONLY));
        assertFalse(SessionModelSourcePolicy.isEnabled(SourceKind.GAME_SERVER, ActiveSessionMode.LOCAL_ONLY));
        assertFalse(SessionModelSourcePolicy.isEnabled(SourceKind.BACKEND, ActiveSessionMode.LOCAL_ONLY));
    }

    @Test
    void sessionBoundSourcesRequireTheirMatchingAuthority() {
        assertTrue(SessionModelSourcePolicy.isEnabled(SourceKind.GAME_SERVER, ActiveSessionMode.GAME_SERVER));
        assertFalse(SessionModelSourcePolicy.isEnabled(SourceKind.BACKEND, ActiveSessionMode.GAME_SERVER));
        assertTrue(SessionModelSourcePolicy.isEnabled(SourceKind.BACKEND, ActiveSessionMode.BACKEND));
        assertFalse(SessionModelSourcePolicy.isEnabled(SourceKind.GAME_SERVER, ActiveSessionMode.BACKEND));
        assertTrue(SessionModelSourcePolicy.isEnabled(SourceKind.EXTERNAL, ActiveSessionMode.GAME_SERVER));
        assertTrue(SessionModelSourcePolicy.isEnabled(SourceKind.EXTERNAL, ActiveSessionMode.BACKEND));
    }
}
