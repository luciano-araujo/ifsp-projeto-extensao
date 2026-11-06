package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TeamTest {

    @Test
    void onCreate_shouldSetCreatedAt() {
        Team team = new Team();

        team.onCreate();

        assertNotNull(team.getCreatedAt());
    }

    @Test
    void setters_shouldExposeValues() {
        Team team = new Team();
        team.setId(10L);
        team.setName("Equipe");

        assertEquals(10L, team.getId());
        assertEquals("Equipe", team.getName());
    }
}
