package com.elfmcys.ysm.network.protocol;

import com.elfmcys.ysm.model.catalog.DefaultAnimationKey;
import com.elfmcys.ysm.proto.network.protocol.v0.HandshakeV0;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultAnimationNegotiationTest {
    @Test
    void acceptsSupersetAndReportsMissingNames() {
        var main = new DefaultAnimationKey("player/main", "idle");
        var walk = new DefaultAnimationKey("player/main", "walk");

        assertEquals(Set.of(), DefaultAnimationNegotiation.missing(
                Set.of(main), List.of(name(main), name(walk))));
        assertEquals(Set.of(walk), DefaultAnimationNegotiation.missing(
                List.of(name(main), name(walk)), Set.of(main)));
    }

    @Test
    void rejectsDuplicateAndUnsortedWireLists() {
        var main = new DefaultAnimationKey("player/main", "idle");
        var walk = new DefaultAnimationKey("player/main", "walk");

        assertThrows(IllegalArgumentException.class, () -> DefaultAnimationNegotiation.missing(
                List.of(name(main), name(main)), Set.of(main)));
        assertThrows(IllegalArgumentException.class, () -> DefaultAnimationNegotiation.missing(
                Set.of(main, walk), List.of(name(walk), name(main))));
    }

    private static HandshakeV0.AnimationName name(DefaultAnimationKey key) {
        return HandshakeV0.AnimationName.newInstance()
                .setDomain(key.domain()).setName(key.name());
    }
}
