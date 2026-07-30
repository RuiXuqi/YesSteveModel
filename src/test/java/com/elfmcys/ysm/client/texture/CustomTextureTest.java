package com.elfmcys.ysm.client.texture;

import com.elfmcys.ysm.client.model.ModelResourceFailureGate;
import com.elfmcys.ysm.format.schema.file.PBRImageSources;
import com.elfmcys.ysm.natives.image.ImageSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CustomTextureTest {
    private static final ImageSource UNUSED_SOURCE = () -> {
        throw new IOException("unused");
    };

    @Test
    void closeAllowsTheSameTextureAndItsPbrComponentsToLoadAgain() {
        var workers = new QueuedExecutor();
        var texture = new CustomPBRTextureSet(
                new PBRImageSources(UNUSED_SOURCE, UNUSED_SOURCE, UNUSED_SOURCE), workers);

        loadAll(texture);
        assertEquals(3, workers.size());

        texture.close();
        loadAll(texture);
        assertEquals(6, workers.size());
    }

    @Test
    void realFailureRemainsFrozenAcrossClose() {
        var attempts = new AtomicInteger();
        var failure = new AtomicReference<Throwable>();
        Executor rejectingWorkers = ignored -> {
            attempts.incrementAndGet();
            throw new RejectedExecutionException("rejected");
        };
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
        var texture = new CustomTexture(UNUSED_SOURCE, rejectingWorkers, gate);

        texture.load(null);
        var firstFailure = texture.failure().orElseThrow();
        texture.close();
        texture.load(null);

        assertEquals(1, attempts.get());
        assertSame(firstFailure, texture.failure().orElseThrow());
        assertSame(firstFailure, failure.get());
    }

    private static void loadAll(CustomPBRTextureSet texture) {
        texture.load(null);
        texture.getNormal().load(null);
        texture.getSpecular().load(null);
    }

    private static final class QueuedExecutor implements Executor {
        private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        public int size() {
            return tasks.size();
        }
    }
}
