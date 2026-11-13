package br.edu.ifsp.tcc.application.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Embedded
    private TeamSubscription subscription = new TeamSubscription();

    // The personal team created on the first access of a user; it cannot be deleted
    @Column(nullable = false)
    private boolean personal = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public TeamSubscription getSubscription() { return subscription; }
    public void setSubscription(TeamSubscription subscription) { this.subscription = subscription; }

    public boolean isPersonal() { return personal; }
    public void setPersonal(boolean personal) { this.personal = personal; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
