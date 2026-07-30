package com.elfmcys.ysm.network.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolMessageRegistryTest {
    @Test
    void indexesStableIdAndType() {
        var spec = new ProtocolMessageSpec<>(1, MessageDirection.CLIENT_TO_SERVER, String.class, 1024);
        var registry = ProtocolMessageRegistry.builder().add(spec).build();

        assertEquals(spec, registry.find(1).orElseThrow());
        assertEquals(spec, registry.find(String.class).orElseThrow());
    }

    @Test
    void rejectsDuplicateIdsAndTypes() {
        var first = new ProtocolMessageSpec<>(1, MessageDirection.CLIENT_TO_SERVER, String.class, 1024);
        var duplicateId = new ProtocolMessageSpec<>(1, MessageDirection.SERVER_TO_CLIENT, Integer.class, 1024);
        var duplicateType = new ProtocolMessageSpec<>(2, MessageDirection.SERVER_TO_CLIENT, String.class, 1024);

        assertThrows(IllegalArgumentException.class,
                () -> ProtocolMessageRegistry.builder().add(first).add(duplicateId));
        assertThrows(IllegalArgumentException.class,
                () -> ProtocolMessageRegistry.builder().add(first).add(duplicateType));
    }

    @Test
    void frozenMessageTableUsesEveryIdFromZeroThroughTwentyOne() {
        for (var id = 0; id <= 21; id++) {
            assertEquals(id, ProtocolMessages.REGISTRY.find(id).orElseThrow().id());
        }
        assertTrue(ProtocolMessages.REGISTRY.find(22).isEmpty());
        assertEquals(MessageDirection.CLIENT_TO_SERVER,
                ProtocolMessages.REGISTRY.find(ProtocolMessages.PLAYER_STATE_REPORT_ID)
                        .orElseThrow().direction());
        assertEquals(MessageDirection.SERVER_TO_CLIENT,
                ProtocolMessages.REGISTRY.find(ProtocolMessages.PLAYER_STATE_UPDATE_ID)
                        .orElseThrow().direction());
        assertEquals(MessageDirection.CLIENT_TO_SERVER,
                ProtocolMessages.REGISTRY.find(ProtocolMessages.ASSET_TRANSFER_RELEASE_ID)
                        .orElseThrow().direction());
    }
}
