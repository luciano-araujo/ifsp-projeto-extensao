package br.edu.ifsp.tcc.application.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "idea_votes")
public class IdeaVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kanban_item_id", nullable = false)
    private KanbanItem kanbanItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public KanbanItem getKanbanItem() { return kanbanItem; }
    public void setKanbanItem(KanbanItem kanbanItem) { this.kanbanItem = kanbanItem; }

    public Long getKanbanItemId() { return kanbanItem != null ? kanbanItem.getId() : null; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Long getUserId() { return user != null ? user.getId() : null; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
