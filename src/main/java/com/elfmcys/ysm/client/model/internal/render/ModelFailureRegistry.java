package com.elfmcys.ysm.client.model.internal.render;

import com.elfmcys.ysm.client.model.catalog.ModelContentVersion;
import com.elfmcys.ysm.client.model.ModelResourceFailureGate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Process-local negative cache. A real failure remains frozen for its content version. */
public final class ModelFailureRegistry {
    private final Map<FailureKey, FailureRecord> failures = new ConcurrentHashMap<>();

    public Optional<FailureRecord> findTarget(ModelRenderTargetRequestKey key) {
        return Optional.ofNullable(failures.get(FailureKey.target(key)));
    }

    public FailureRecord recordTarget(ModelRenderTargetRequestKey key, Throwable cause) {
        return record(FailureKey.target(key), cause);
    }

    public Optional<FailureRecord> find(ModelContentVersion version, Stage stage, String resource) {
        return Optional.ofNullable(failures.get(new FailureKey(version, stage, resource)));
    }

    public FailureRecord record(ModelContentVersion version, Stage stage, String resource,
                                Throwable cause) {
        return record(new FailureKey(version, stage, resource), cause);
    }

    public ModelResourceFailureGate gate(ModelContentVersion version, Stage stage, String resource,
                                         Consumer<Throwable> firstFailure) {
        var key = new FailureKey(version, stage, resource);
        return new ModelResourceFailureGate() {
            @Override
            public Optional<Throwable> failure() {
                var failure = failures.get(key);
                return failure == null ? Optional.empty() : Optional.of(failure.cause());
            }

            @Override
            public void fail(Throwable cause) {
                var created = new FailureRecord(cause, Instant.now());
                var current = failures.putIfAbsent(key, created);
                if (current == null) {
                    firstFailure.accept(cause);
                } else {
                    current.increment();
                }
            }
        };
    }

    public void clear(ModelContentVersion version) {
        failures.keySet().removeIf(key -> key.contentVersion().equals(version));
    }

    public boolean hasMatching(ModelContentVersion version, Stage stage,
                               Predicate<String> resource) {
        return failures.keySet().stream().anyMatch(key -> key.contentVersion().equals(version)
                && key.stage() == stage && resource.test(key.resource()));
    }

    public void clear() {
        failures.clear();
    }

    int size() {
        return failures.size();
    }

    private FailureRecord record(FailureKey key, Throwable cause) {
        return failures.compute(key, (ignored, current) -> {
            if (current == null) {
                return new FailureRecord(cause, Instant.now());
            }
            current.increment();
            return current;
        });
    }

    public enum Stage {
        TARGET,
        TEXTURE,
        ANIMATION
    }

    public record FailureKey(ModelContentVersion contentVersion, Stage stage, String resource) {
        private static FailureKey target(ModelRenderTargetRequestKey key) {
            return new FailureKey(key.contentVersion(), Stage.TARGET,
                    key.renderTargetId() + "\0" + key.textureName());
        }
    }

    public static final class FailureRecord {
        private final Throwable cause;
        private final Instant firstObservedAt;
        private final AtomicInteger observations = new AtomicInteger(1);

        private FailureRecord(Throwable cause, Instant firstObservedAt) {
            this.cause = cause;
            this.firstObservedAt = firstObservedAt;
        }

        public Throwable cause() {
            return cause;
        }

        public Instant firstObservedAt() {
            return firstObservedAt;
        }

        public int observations() {
            return observations.get();
        }

        private void increment() {
            observations.incrementAndGet();
        }
    }
}
