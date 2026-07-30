package com.elfmcys.ysm.network.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssetTransferBudgetTest {
    @Test
    void enforcesAggregateBytesAndTransferCountAndReleasesIdempotently() {
        var budget = new AssetTransferBudget(2, 100);
        var first = budget.reserve(20, 30);
        var second = budget.reserve(10, 20);

        assertEquals(2, budget.activeTransfers());
        assertEquals(80, budget.reservedBytes());
        assertThrows(IllegalStateException.class, () -> budget.reserve(1, 1));

        first.close();
        first.close();
        assertEquals(1, budget.activeTransfers());
        assertEquals(30, budget.reservedBytes());
        assertThrows(IllegalStateException.class, () -> budget.reserve(40, 31));

        try (var replacement = budget.reserve(40, 30)) {
            assertEquals(100, budget.reservedBytes());
        }
        second.close();
        assertEquals(0, budget.activeTransfers());
        assertEquals(0, budget.reservedBytes());
    }
}
