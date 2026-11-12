package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CardMovementTest {

    @Test
    void onCreate_shouldSetEnteredAtOnlyWhenMissing() {
        CardMovement movement = new CardMovement();
        movement.onCreate();
        assertNotNull(movement.getEnteredAt());

        LocalDateTime fixed = LocalDateTime.of(2026, 11, 1, 14, 10);
        movement.setEnteredAt(fixed);
        movement.onCreate();
        assertEquals(fixed, movement.getEnteredAt());
    }

    @Test
    void setters_shouldExposeValuesAndIds() {
        KanbanItem item = new KanbanItem();
        item.setId(5L);
        BoardColumn column = new BoardColumn();
        column.setId(2L);
        User author = new User();
        CardMovement movement = new CardMovement();
        assertNull(movement.getKanbanItemId());
        assertNull(movement.getColumnId());

        movement.setId(9L);
        movement.setKanbanItem(item);
        movement.setColumn(column);
        movement.setColumnName("Roteiro");
        movement.setMovedBy(author);

        assertEquals(9L, movement.getId());
        assertSame(item, movement.getKanbanItem());
        assertEquals(5L, movement.getKanbanItemId());
        assertSame(column, movement.getColumn());
        assertEquals(2L, movement.getColumnId());
        assertEquals("Roteiro", movement.getColumnName());
        assertSame(author, movement.getMovedBy());
    }
}
