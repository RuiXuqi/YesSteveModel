package com.elfmcys.ysm.util;

import java.util.ArrayList;

/** Reverse-order rollback for resources under construction. */
public final class ResourceTransaction implements AutoCloseable {
    private final ArrayList<AutoCloseable> resources = new ArrayList<>();
    private boolean committed;

    public <T extends AutoCloseable> T own(T resource) {
        if (committed) {
            throw new IllegalStateException("Resource transaction is already committed");
        }
        resources.add(resource);
        return resource;
    }

    public void commit() {
        committed = true;
        resources.clear();
    }

    @Override
    public void close() {
        if (committed) {
            return;
        }
        for (var index = resources.size() - 1; index >= 0; index--) {
            try {
                resources.get(index).close();
            } catch (Exception ignored) {
            }
        }
        resources.clear();
    }
}
