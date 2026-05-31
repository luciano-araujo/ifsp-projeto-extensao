package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModuleTest {

    @Test
    void getKanbanItemId_shouldReturnIdWhenSet() {
        KanbanItem item = new KanbanItem();
        item.setId(5L);
        Module module = new Module();
        module.setKanbanItem(item);
        assertEquals(5L, module.getKanbanItemId());
    }

    @Test
    void getKanbanItemId_shouldReturnNullWhenNotSet() {
        Module module = new Module();
        assertNull(module.getKanbanItemId());
    }

    @Test
    void defaults_shouldBeCorrect() {
        Module module = new Module();
        assertEquals(0, module.getSortOrder());
        assertNotNull(module.getLessons());
        assertTrue(module.getLessons().isEmpty());
    }

    @Test
    void settersAndGetters_shouldWork() {
        Module module = new Module();
        module.setId(1L);
        module.setTitle("Introducao");
        module.setSortOrder(2);
        assertEquals(1L, module.getId());
        assertEquals("Introducao", module.getTitle());
        assertEquals(2, module.getSortOrder());
    }
}
