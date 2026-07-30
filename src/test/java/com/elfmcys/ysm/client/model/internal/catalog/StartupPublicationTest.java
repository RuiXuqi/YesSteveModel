package com.elfmcys.ysm.client.model.internal.catalog;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartupPublicationTest {
    @Test
    void promotionPublishesLatestStagedValueBeforeAdmittingLiveUpdates() {
        var publication = new StartupPublication<Integer>();
        assertEquals(StartupPublication.Disposition.STAGED, publication.stage(1));
        assertEquals(StartupPublication.Disposition.STAGED, publication.stage(2));

        var published = new AtomicInteger();
        assertEquals(3, publication.promote(value -> value + 1, published::set));
        assertEquals(3, published.get());
        assertTrue(publication.live());
        assertEquals(StartupPublication.Disposition.LIVE, publication.stage(4));
    }

    @Test
    void transitionAtPromotionBoundaryIsEitherIncludedOrMarkedLive() throws Exception {
        var publication = new StartupPublication<Integer>();
        publication.stage(1);
        var publisherEntered = new CountDownLatch(1);
        var releasePublisher = new CountDownLatch(1);
        var disposition = new AtomicReference<StartupPublication.Disposition>();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var promotion = executor.submit(() -> publication.promote(value -> value, value -> {
                publisherEntered.countDown();
                try {
                    assertTrue(releasePublisher.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
            }));
            assertTrue(publisherEntered.await(5, TimeUnit.SECONDS));
            var transition = executor.submit(() -> disposition.set(publication.stage(2)));
            releasePublisher.countDown();
            assertEquals(1, promotion.get(5, TimeUnit.SECONDS));
            transition.get(5, TimeUnit.SECONDS);
            assertEquals(StartupPublication.Disposition.LIVE, disposition.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void closeRejectsPendingAndLatePublication() {
        var publication = new StartupPublication<Integer>();
        publication.stage(1);
        publication.close();

        assertFalse(publication.live());
        assertEquals(StartupPublication.Disposition.CLOSED, publication.stage(2));
        assertThrows(IllegalStateException.class,
                () -> publication.promote(value -> value, ignored -> { }));
    }
}
