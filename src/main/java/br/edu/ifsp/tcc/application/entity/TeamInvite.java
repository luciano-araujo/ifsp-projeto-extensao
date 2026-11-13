package br.edu.ifsp.tcc.application.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "team_invites")
public class TeamInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamRole role = TeamRole.CREATOR;

    // SHA-256 of the token, in hex; the token itself is never stored
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InviteStatus status = InviteStatus.PENDING;

    // Null after the inviter deletes the account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    private User invitedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /** Usable to join the team: not accepted yet and not expired. */
    public boolean isOpen() {
        return status == InviteStatus.PENDING && !isExpired();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public TeamRole getRole() { return role; }
    public void setRole(TeamRole role) { this.role = role; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public InviteStatus getStatus() { return status; }
    public void setStatus(InviteStatus status) { this.status = status; }

    public User getInvitedBy() { return invitedBy; }
    public void setInvitedBy(User invitedBy) { this.invitedBy = invitedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
