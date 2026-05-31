package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KanbanItemTest {

    @Test
    void getUserId_shouldReturnUserIdWhenUserIsSet() {
        User user = new User();
        user.setId(42L);

        KanbanItem item = new KanbanItem();
        item.setUser(user);

        assertEquals(42L, item.getUserId());
    }

    @Test
    void getUserId_shouldReturnNullWhenUserIsNull() {
        KanbanItem item = new KanbanItem();
        assertNull(item.getUserId());
    }

    @Test
    void defaults_shouldBeCorrect() {
        KanbanItem item = new KanbanItem();
        assertEquals(KanbanItemState.IDEATION, item.getState());
        assertEquals(0, item.getProgress());
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        item.setTitle("Title");
        item.setDescription("Desc");
        item.setState(KanbanItemState.REVIEW);
        item.setTargetAudience("Devs");
        item.setPedagogicalObjective("Learn stuff");
        item.setProgress(75);

        assertEquals(1L, item.getId());
        assertEquals("Title", item.getTitle());
        assertEquals("Desc", item.getDescription());
        assertEquals(KanbanItemState.REVIEW, item.getState());
        assertEquals("Devs", item.getTargetAudience());
        assertEquals("Learn stuff", item.getPedagogicalObjective());
        assertEquals(75, item.getProgress());
    }
}
