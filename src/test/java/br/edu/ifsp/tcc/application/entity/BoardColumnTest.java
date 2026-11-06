package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardColumnTest {

    @Test
    void onCreate_shouldSetCreatedAt() {
        BoardColumn column = new BoardColumn();

        column.onCreate();

        assertNotNull(column.getCreatedAt());
    }

    @Test
    void setters_shouldExposeValues() {
        Team team = new Team();
        BoardColumn column = new BoardColumn();
        column.setId(1L);
        column.setTeam(team);
        column.setName("Pesquisa");
        column.setSortOrder(3);

        assertEquals(1L, column.getId());
        assertSame(team, column.getTeam());
        assertEquals("Pesquisa", column.getName());
        assertEquals(3, column.getSortOrder());
    }
}
