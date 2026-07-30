package com.elfmcys.ysm.client.texture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextureLoadStateTest {
    @Test
    void deactivateInvalidatesOnlyTheCurrentRegistration() {
        var state = new TextureLoadState();
        var first = state.begin();
        var firstTask = new CompletableFuture<Void>();
        assertTrue(state.attach(first.generation(), firstTask));

        assertSame(firstTask, state.deactivate());
        assertFalse(state.isCurrent(first.generation()));

        var second = state.begin();
        assertTrue(state.isCurrent(second.generation()));
        assertNull(second.previous());
    }

    @Test
    void staleCompletionAndFailureCannotAffectReplacement() {
        var state = new TextureLoadState();
        var first = state.begin();
        var firstTask = new CompletableFuture<Void>();
        assertTrue(state.attach(first.generation(), firstTask));

        var second = state.begin();
        var secondTask = new CompletableFuture<Void>();
        assertSame(firstTask, second.previous());
        assertTrue(state.attach(second.generation(), secondTask));

        state.complete(first.generation(), firstTask);
        assertFalse(state.fail(first.generation(), new IOException("stale")));
        assertSame(secondTask, state.deactivate());
        assertTrue(state.failure().isEmpty());
    }

    @Test
    void currentFailureSurvivesDeactivateAndBlocksRestart() {
        var state = new TextureLoadState();
        var current = state.begin();
        var failure = new IOException("broken");

        assertTrue(state.fail(current.generation(), failure));
        assertSame(failure, state.failure().orElseThrow());
        assertNull(state.deactivate());
        assertNull(state.begin());
    }
}
