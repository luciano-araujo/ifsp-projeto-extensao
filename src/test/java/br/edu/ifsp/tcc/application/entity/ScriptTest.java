package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScriptTest {

    @Test
    void getKanbanItemId_shouldReturnIdWhenSet() {
        KanbanItem item = new KanbanItem();
        item.setId(42L);
        Script script = new Script();
        script.setKanbanItem(item);
        assertEquals(42L, script.getKanbanItemId());
    }

    @Test
    void getKanbanItemId_shouldReturnNullWhenNotSet() {
        Script script = new Script();
        assertNull(script.getKanbanItemId());
    }

    @Test
    void settersAndGetters_shouldWork() {
        Script script = new Script();
        script.setId(1L);
        script.setContent("<p>Hello</p>");
        assertEquals(1L, script.getId());
        assertEquals("<p>Hello</p>", script.getContent());
    }
}
