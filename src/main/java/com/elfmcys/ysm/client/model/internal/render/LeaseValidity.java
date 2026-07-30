package com.elfmcys.ysm.client.model.internal.render;

import java.util.concurrent.atomic.AtomicBoolean;

final class LeaseValidity {
    private final AtomicBoolean current = new AtomicBoolean(true);

    boolean isCurrent() {
        return current.get();
    }

    void revoke() {
        current.set(false);
    }
}
