package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KanbanItemEntityTest {

    @Test
    void relatedIds_shouldBeNullWithoutRelations() {
        KanbanItem item = new KanbanItem();

        assertNull(item.getUserId());
        assertNull(item.getTeamId());
        assertNull(item.getColumnId());
    }

    @Test
    void relatedIds_shouldReturnRelationIds() {
        KanbanItem item = new KanbanItem();
        User user = new User();
        user.setId(1L);
        Team team = new Team();
        team.setId(10L);
        BoardColumn column = new BoardColumn();
        column.setId(2L);
        item.setUser(user);
        item.setTeam(team);
        item.setColumn(column);

        assertEquals(1L, item.getUserId());
        assertEquals(10L, item.getTeamId());
        assertEquals(2L, item.getColumnId());
    }

    @Test
    void onCreate_shouldSetCreatedAndUpdatedAt() {
        KanbanItem item = new KanbanItem();

        item.onCreate();

        assertNotNull(item.getCreatedAt());
        assertNotNull(item.getUpdatedAt());
    }

    @Test
    void onCreate_shouldGenerateUuidOnlyWhenMissing() {
        KanbanItem item = new KanbanItem();
        item.onCreate();
        String generated = item.getUuid();
        assertEquals(36, generated.length());

        item.onCreate();
        assertEquals(generated, item.getUuid());

        KanbanItem withUuid = new KanbanItem();
        withUuid.setUuid("fixo");
        withUuid.onCreate();
        assertEquals("fixo", withUuid.getUuid());
    }

    @Test
    void onUpdate_shouldRefreshUpdatedAt() {
        KanbanItem item = new KanbanItem();

        item.onUpdate();

        assertNotNull(item.getUpdatedAt());
    }

    @Test
    void setters_shouldExposeValues() {
        KanbanItem item = new KanbanItem();
        item.setTitle("Projeto");
        item.setState(KanbanItemState.IN_PRODUCTION);
        item.setTag("Aula");

        assertEquals("Projeto", item.getTitle());
        assertEquals(KanbanItemState.IN_PRODUCTION, item.getState());
        assertEquals("Aula", item.getTag());
    }
}
