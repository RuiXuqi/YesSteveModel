package com.elfmcys.ysm.network.protocol;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** Tracks the first occurrence of a diagnostic key within a connection lifetime. */
public final class FirstOccurrenceTracker<K> {
    private final Set<K> observed = ConcurrentHashMap.newKeySet();

    public boolean first(K key) {
        return observed.add(key);
    }

    public void removeIf(Predicate<K> predicate) {
        observed.removeIf(predicate);
    }

    public void clear() {
        observed.clear();
    }
}
