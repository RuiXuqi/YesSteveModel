package com.elfmcys.ysm.client.model.internal.render;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalPublicationGateTest {
    @Test
    void closeWaitsForAnAdmittedPublicationAndRejectsLatePublication() throws Exception {
        var gate = new FinalPublicationGate();
        var publicationEntered = new CountDownLatch(1);
        var releasePublication = new CountDownLatch(1);
        var publications = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var publication = executor.submit(() -> gate.publish(() -> {
                publicationEntered.countDown();
                try {
                    assertTrue(releasePublication.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
                publications.incrementAndGet();
            }));
            assertTrue(publicationEntered.await(5, TimeUnit.SECONDS));

            var close = executor.submit(gate::close);
            assertFalse(close.isDone());
            releasePublication.countDown();

            publication.get(5, TimeUnit.SECONDS);
            assertTrue(close.get(5, TimeUnit.SECONDS));
            assertEquals(1, publications.get());
            assertThrows(java.util.concurrent.CancellationException.class,
                    () -> gate.publish(publications::incrementAndGet));
            assertEquals(1, publications.get());
            assertFalse(gate.close());
        } finally {
            releasePublication.countDown();
            executor.shutdownNow();
        }
    }
}
