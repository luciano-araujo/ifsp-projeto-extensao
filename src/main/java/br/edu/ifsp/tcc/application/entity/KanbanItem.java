package br.edu.ifsp.tcc.application.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "kanban_items")
public class KanbanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KanbanItemState state = KanbanItemState.IDEATION;

    @Column(name = "target_audience")
    private String targetAudience;

    @Column(name = "pedagogical_objective", columnDefinition = "TEXT")
    private String pedagogicalObjective;

    @Column(nullable = false)
    private Integer progress = 0;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_id")
    private BoardColumn column;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(length = 40)
    private String tag;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "due_date")
    private LocalDate dueDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public KanbanItemState getState() { return state; }
    public void setState(KanbanItemState state) { this.state = state; }

    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }

    public String getPedagogicalObjective() { return pedagogicalObjective; }
    public void setPedagogicalObjective(String pedagogicalObjective) { this.pedagogicalObjective = pedagogicalObjective; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Long getUserId() { return user != null ? user.getId() : null; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public Long getTeamId() { return team != null ? team.getId() : null; }

    public BoardColumn getColumn() { return column; }
    public void setColumn(BoardColumn column) { this.column = column; }

    public Long getColumnId() { return column != null ? column.getId() : null; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
