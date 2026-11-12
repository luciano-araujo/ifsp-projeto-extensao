package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TagTest {

    @Test
    void onCreate_shouldSetCreatedAt() {
        Tag tag = new Tag();

        tag.onCreate();

        assertNotNull(tag.getCreatedAt());
    }

    @Test
    void setters_shouldExposeValuesAndTeamId() {
        Team team = new Team();
        team.setId(10L);
        Tag tag = new Tag();
        assertNull(tag.getTeamId());

        tag.setId(1L);
        tag.setTeam(team);
        tag.setName("Tutorial");
        tag.setColor("#2E7D32");

        assertEquals(1L, tag.getId());
        assertSame(team, tag.getTeam());
        assertEquals(10L, tag.getTeamId());
        assertEquals("Tutorial", tag.getName());
        assertEquals("#2E7D32", tag.getColor());
    }
}
