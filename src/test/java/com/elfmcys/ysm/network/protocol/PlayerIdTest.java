package com.elfmcys.ysm.network.protocol;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PlayerIdTest {
    @Test
    void forbiddenEntityRefsNeverExposePlayerId() {
        var encoder = new EntityRefEncoder(ignored -> new PlayerId(new byte[PlayerId.SIZE]));
        encoder.observePlayer(12, UUID.randomUUID());

        var encoded = encoder.encodePlayer(12);
        encoded.sentCommit().run();

        assertFalse(encoded.value().hasPlayerId());
        assertFalse(EntityRefEncoder.encodeNonPlayer(13).value().hasPlayerId());
    }
}
