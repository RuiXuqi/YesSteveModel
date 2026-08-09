package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.model.domain.Hash256;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ControlHandlerTest {
    @Test
    void absentAuthorizedModelHashesAreAnEmptySet() {
        var snapshot = ControlHandler.authorizedModels(Set.of(), 1);

        assertFalse(snapshot.hasModelHashes());
        assertEquals(Set.of(), ControlHandler.readHashSet(snapshot.getModelHashes()));
    }

    @Test
    void absentStarredModelHashesAreAnEmptySet() {
        var snapshot = ControlHandler.starredModels(Set.of(), 1);

        assertFalse(snapshot.hasModelHashes());
        assertEquals(Set.of(), ControlHandler.readHashSet(snapshot.getModelHashes()));
    }

    @Test
    void presentModelHashesRoundTrip() {
        var bytes = new byte[Hash256.SIZE];
        bytes[0] = 1;
        var hash = new Hash256(bytes);
        var snapshot = ControlHandler.authorizedModels(Set.of(hash), 1);

        assertEquals(Set.of(hash), ControlHandler.readHashSet(snapshot.getModelHashes()));
    }
}
