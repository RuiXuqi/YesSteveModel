package com.elfmcys.ysm.model.catalog;

final class HashProbeBudget {
    private final int missLimit;
    private int misses;

    HashProbeBudget(int missLimit) {
        if (missLimit < 0) {
            throw new IllegalArgumentException("missLimit must not be negative");
        }
        this.missLimit = missLimit;
    }

    boolean mayProbe() {
        return misses < missLimit;
    }

    void recordMiss() {
        if (misses < missLimit) {
            misses++;
        }
    }
}
