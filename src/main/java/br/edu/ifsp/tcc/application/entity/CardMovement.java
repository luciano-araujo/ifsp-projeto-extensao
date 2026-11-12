package br.edu.ifsp.tcc.application.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** One entry of a card's history: the card entered a column. The column name is a snapshot. */
@Entity
@Table(name = "card_movements")
public class CardMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kanban_item_id", nullable = false)
    private KanbanItem kanbanItem;

    // Null after the column is deleted; columnName keeps what it was called
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_id")
    private BoardColumn column;

    @Column(name = "column_name", nullable = false, length = 120)
    private String columnName;

    // Null for the migrated history and after the author deletes the account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moved_by")
    private User movedBy;

    @Column(name = "entered_at", nullable = false)
    private LocalDateTime enteredAt;

    @PrePersist
    protected void onCreate() {
        if (enteredAt == null) enteredAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public KanbanItem getKanbanItem() { return kanbanItem; }
    public void setKanbanItem(KanbanItem kanbanItem) { this.kanbanItem = kanbanItem; }

    public Long getKanbanItemId() { return kanbanItem != null ? kanbanItem.getId() : null; }

    public BoardColumn getColumn() { return column; }
    public void setColumn(BoardColumn column) { this.column = column; }

    public Long getColumnId() { return column != null ? column.getId() : null; }

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    public User getMovedBy() { return movedBy; }
    public void setMovedBy(User movedBy) { this.movedBy = movedBy; }

    public LocalDateTime getEnteredAt() { return enteredAt; }
    public void setEnteredAt(LocalDateTime enteredAt) { this.enteredAt = enteredAt; }
}
