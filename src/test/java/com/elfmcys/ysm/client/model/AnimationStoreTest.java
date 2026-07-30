package com.elfmcys.ysm.client.model;

import com.elfmcys.ysm.geckolib3.core.builder.Animation;
import com.elfmcys.ysm.geckolib3.core.builder.LoopType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AnimationStoreTest {
    @Test
    void fallsBackWhenLocalNameIsAbsentOrPayloadIsMissing() {
        var fallbackAnimation = animation("shared");
        var fallbackOnly = animation("fallback-only");
        var fallback = AnimationStore.eager(Map.of(
                "shared", fallbackAnimation,
                "fallback-only", fallbackOnly));
        var store = AnimationStore.lazy(List.of("shared"),
                name -> null, ignored -> animation("unused"), fallback);

        assertSame(fallbackAnimation, store.get("shared"));
        assertSame(fallbackOnly, store.get("fallback-only"));
    }

    @Test
    void bindsPresentLocalAnimationBeforeUsingFallback() {
        var fallbackAnimation = animation("shared");
        var localAnimation = animation("shared");
        var fallback = AnimationStore.eager(Map.of("shared", fallbackAnimation));
        var store = AnimationStore.lazy(List.of("shared"),
                name -> mixel.asset.model.data.AnimationOuterClass.Animation
                        .newInstance().setName(name),
                ignored -> localAnimation, fallback);

        assertSame(localAnimation, store.get("shared"));
    }

    @Test
    void failedAnimationFallsBackAndDoesNotRetry() {
        var fallbackAnimation = animation("shared");
        var fallback = AnimationStore.eager(Map.of("shared", fallbackAnimation));
        var loads = new AtomicInteger();
        var failure = new AtomicReference<Throwable>();
        var gate = new ModelResourceFailureGate() {
            @Override
            public Optional<Throwable> failure() {
                return Optional.ofNullable(failure.get());
            }

            @Override
            public void fail(Throwable cause) {
                failure.compareAndSet(null, cause);
            }
        };
        var store = AnimationStore.lazy(List.of("shared"), name -> {
            loads.incrementAndGet();
            throw new java.io.IOException("broken");
        }, ignored -> animation("unused"), fallback, ignored -> gate);

        assertSame(fallbackAnimation, store.get("shared"));
        assertSame(fallbackAnimation, store.get("shared"));
        assertEquals(1, loads.get());
        assertEquals(AnimationStore.State.FAILED, store.state("shared"));

        var reconstructed = AnimationStore.lazy(List.of("shared"), name -> {
            loads.incrementAndGet();
            return null;
        }, ignored -> animation("unused"), fallback, ignored -> gate);
        assertSame(fallbackAnimation, reconstructed.get("shared"));
        assertEquals(1, loads.get());
    }

    private static Animation animation(String name) {
        return new Animation(name, 0, LoopType.PLAY_ONCE, null,
                List.of(), List.of(), List.of());
    }
}
