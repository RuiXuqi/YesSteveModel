package com.elfmcys.ysm.capability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelInfoCapabilityTest {
    @Test
    void cloneKeepsTheOutboundRevisionMonotonic() {
        var source = new ModelInfoCapability();
        assertEquals(1, source.nextStateRevision());
        assertEquals(2, source.nextStateRevision());

        var destination = new ModelInfoCapability();
        destination.moveFrom(source);

        assertEquals(3, destination.nextStateRevision());
    }
}
