package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TeamSubscriptionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 13, 12, 0);

    private TeamSubscription trial(LocalDateTime expiresAt) {
        TeamSubscription s = new TeamSubscription();
        s.setPlan(PlanType.PRO);
        s.setSource(PlanSource.TRIAL);
        s.setExpiresAt(expiresAt);
        return s;
    }

    @Test
    void newTeam_shouldBeOnGratuitoFree() {
        TeamSubscription s = new TeamSubscription();
        assertEquals(PlanType.GRATUITO, s.effectivePlan(NOW));
        assertEquals(PlanSource.FREE, s.effectiveSource(NOW));
        assertNull(s.getTrialUsedAt());
        assertFalse(s.isExpired(NOW));
    }

    @Test
    void trial_shouldBeInForceUntilTheLastSecondAndThenFallBackToGratuito() {
        TeamSubscription s = trial(NOW.plusSeconds(1));
        assertEquals(PlanType.PRO, s.effectivePlan(NOW));
        assertEquals(PlanSource.TRIAL, s.effectiveSource(NOW));
        assertEquals(NOW.plusSeconds(1), s.effectiveExpiresAt(NOW));

        assertEquals(PlanType.GRATUITO, s.effectivePlan(NOW.plusSeconds(1)));
        assertEquals(PlanSource.FREE, s.effectiveSource(NOW.plusSeconds(1)));
        assertNull(s.effectiveExpiresAt(NOW.plusSeconds(1)));
        assertTrue(s.isExpired(NOW.plusDays(1)));
    }

    @Test
    void planWithoutEndDate_shouldNeverExpire() {
        TeamSubscription s = trial(null);
        s.setSource(PlanSource.MANUAL);
        assertEquals(PlanType.PRO, s.effectivePlan(NOW.plusYears(10)));
    }
}
