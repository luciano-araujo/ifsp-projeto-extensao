package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardTaskTest {

    @Test
    void getKanbanItemId_shouldReturnNullWithoutCard() {
        CardTask task = new CardTask();

        assertNull(task.getKanbanItemId());
    }

    @Test
    void getKanbanItemId_shouldReturnCardId() {
        CardTask task = new CardTask();
        KanbanItem item = new KanbanItem();
        item.setId(5L);
        task.setKanbanItem(item);

        assertEquals(5L, task.getKanbanItemId());
        assertSame(item, task.getKanbanItem());
    }

    @Test
    void onCreate_shouldSetCreatedAt() {
        CardTask task = new CardTask();

        task.onCreate();

        assertNotNull(task.getCreatedAt());
    }

    @Test
    void assigneeAndDueDate_shouldBeOptional() {
        CardTask task = new CardTask();
        assertNull(task.getAssignee());
        assertNull(task.getDueDate());

        TeamMember member = new TeamMember();
        task.setAssignee(member);
        task.setDueDate(java.time.LocalDate.of(2026, 11, 5));

        assertSame(member, task.getAssignee());
        assertEquals(java.time.LocalDate.of(2026, 11, 5), task.getDueDate());
    }

    @Test
    void setters_shouldExposeValues() {
        CardTask task = new CardTask();
        task.setId(1L);
        task.setTitle("Gravar");
        task.setCompleted(true);
        task.setSortOrder(2);

        assertEquals(1L, task.getId());
        assertEquals("Gravar", task.getTitle());
        assertTrue(task.getCompleted());
        assertEquals(2, task.getSortOrder());
    }
}
