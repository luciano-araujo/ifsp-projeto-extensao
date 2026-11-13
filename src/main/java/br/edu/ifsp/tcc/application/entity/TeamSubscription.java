package br.edu.ifsp.tcc.application.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;

/** Plan state of a team. The trial/paid expiry is computed on read: no job downgrades the team. */
@Embeddable
public class TeamSubscription {

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    private PlanType plan = PlanType.GRATUITO;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_source", nullable = false)
    private PlanSource source = PlanSource.FREE;

    @Column(name = "plan_expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "trial_used_at")
    private LocalDateTime trialUsedAt;

    /** True when the plan has an end date that has already passed. */
    public boolean isExpired(LocalDateTime now) {
        return plan != PlanType.GRATUITO && expiresAt != null && !expiresAt.isAfter(now);
    }

    /** The plan in force at the given moment: Gratuito once the trial or the activation has expired. */
    public PlanType effectivePlan(LocalDateTime now) {
        return isExpired(now) ? PlanType.GRATUITO : plan;
    }

    public PlanSource effectiveSource(LocalDateTime now) {
        return isExpired(now) ? PlanSource.FREE : source;
    }

    public LocalDateTime effectiveExpiresAt(LocalDateTime now) {
        return isExpired(now) ? null : expiresAt;
    }

    public PlanType getPlan() { return plan; }
    public void setPlan(PlanType plan) { this.plan = plan; }

    public PlanSource getSource() { return source; }
    public void setSource(PlanSource source) { this.source = source; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getTrialUsedAt() { return trialUsedAt; }
    public void setTrialUsedAt(LocalDateTime trialUsedAt) { this.trialUsedAt = trialUsedAt; }
}
