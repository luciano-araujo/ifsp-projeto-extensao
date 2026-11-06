package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScriptEntityTest {

    @Test
    void getKanbanItemId_shouldReturnNullWithoutItem() {
        Script script = new Script();

        assertNull(script.getKanbanItemId());
    }

    @Test
    void getKanbanItemId_shouldReturnItemId() {
        Script script = new Script();
        KanbanItem item = new KanbanItem();
        item.setId(5L);
        script.setKanbanItem(item);

        assertEquals(5L, script.getKanbanItemId());
        assertSame(item, script.getKanbanItem());
    }

    @Test
    void onCreate_shouldSetCreatedAndUpdatedAt() {
        Script script = new Script();

        script.onCreate();

        assertNotNull(script.getCreatedAt());
        assertNotNull(script.getUpdatedAt());
    }

    @Test
    void onUpdate_shouldRefreshUpdatedAt() {
        Script script = new Script();

        script.onUpdate();

        assertNotNull(script.getUpdatedAt());
    }

    @Test
    void setters_shouldExposeValues() {
        Script script = new Script();
        script.setId(1L);
        script.setContent("<p>roteiro</p>");

        assertEquals(1L, script.getId());
        assertEquals("<p>roteiro</p>", script.getContent());
    }
}
