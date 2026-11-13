package br.edu.ifsp.tcc.application.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {

    private final AtomicLong now = new AtomicLong(1_000_000L);
    private final RateLimitService service = new RateLimitService(10, now::get);

    @Test
    void registerAttempt_shouldAllowUpToTheLimit() {
        assertEquals(0, service.registerAttempt("k", 3));
        assertEquals(0, service.registerAttempt("k", 3));
        assertEquals(0, service.registerAttempt("k", 3));
    }

    @Test
    void registerAttempt_shouldBlockAfterTheLimitAndReportSecondsLeft() {
        for (int i = 0; i < 3; i++) service.registerAttempt("k", 3);
        now.addAndGet(60_000);

        assertEquals(540, service.registerAttempt("k", 3));
        assertEquals(540, service.registerAttempt("k", 3));
    }

    @Test
    void registerAttempt_shouldUseTheWindowOfTheCaller() {
        java.time.Duration hour = java.time.Duration.ofHours(1);
        for (int i = 0; i < 2; i++) assertEquals(0, service.registerAttempt("h", 2, hour));
        now.addAndGet(20 * 60_000L);

        // past the 10-minute service window, still inside the caller's 1-hour window
        assertEquals(40 * 60, service.registerAttempt("h", 2, hour));
        now.addAndGet(40 * 60_000L);
        assertEquals(0, service.registerAttempt("h", 2, hour));
    }

    @Test
    void registerAttempt_shouldReportAtLeastOneSecond() {
        for (int i = 0; i < 3; i++) service.registerAttempt("k", 3);
        now.addAndGet(10 * 60_000L - 1);

        assertEquals(1, service.registerAttempt("k", 3));
    }

    @Test
    void registerAttempt_shouldKeepKeysIndependent() {
        for (int i = 0; i < 3; i++) service.registerAttempt("a", 3);

        assertTrue(service.registerAttempt("a", 3) > 0);
        assertEquals(0, service.registerAttempt("b", 3));
    }

    @Test
    void registerAttempt_shouldStartNewWindowAfterItEnds() {
        for (int i = 0; i < 4; i++) service.registerAttempt("k", 3);
        now.addAndGet(10 * 60_000L);

        assertEquals(0, service.registerAttempt("k", 3));
    }

    @Test
    void publicConstructor_shouldUseSystemClock() {
        RateLimitService real = new RateLimitService(1);
        assertEquals(0, real.registerAttempt("k", 1));
        assertTrue(real.registerAttempt("k", 1) > 0);
    }
}
