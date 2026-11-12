package br.edu.ifsp.tcc.application.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {

    private final AtomicLong now = new AtomicLong(1_000_000L);
    private final RateLimitService service = new RateLimitService(3, 10, now::get);

    @Test
    void registerAttempt_shouldAllowUpToTheLimit() {
        assertEquals(0, service.registerAttempt("k"));
        assertEquals(0, service.registerAttempt("k"));
        assertEquals(0, service.registerAttempt("k"));
    }

    @Test
    void registerAttempt_shouldBlockAfterTheLimitAndReportSecondsLeft() {
        for (int i = 0; i < 3; i++) service.registerAttempt("k");
        now.addAndGet(60_000);

        assertEquals(540, service.registerAttempt("k"));
        assertEquals(540, service.registerAttempt("k"));
    }

    @Test
    void registerAttempt_shouldReportAtLeastOneSecond() {
        for (int i = 0; i < 3; i++) service.registerAttempt("k");
        now.addAndGet(10 * 60_000L - 1);

        assertEquals(1, service.registerAttempt("k"));
    }

    @Test
    void registerAttempt_shouldKeepKeysIndependent() {
        for (int i = 0; i < 3; i++) service.registerAttempt("a");

        assertTrue(service.registerAttempt("a") > 0);
        assertEquals(0, service.registerAttempt("b"));
    }

    @Test
    void registerAttempt_shouldStartNewWindowAfterItEnds() {
        for (int i = 0; i < 4; i++) service.registerAttempt("k");
        now.addAndGet(10 * 60_000L);

        assertEquals(0, service.registerAttempt("k"));
    }

    @Test
    void publicConstructor_shouldUseSystemClock() {
        RateLimitService real = new RateLimitService(1, 1);
        assertEquals(0, real.registerAttempt("k"));
        assertTrue(real.registerAttempt("k") > 0);
    }
}
