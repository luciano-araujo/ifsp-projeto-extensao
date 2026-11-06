package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CourseTest {

    @Test
    void getTeamId_shouldReturnNullWithoutTeam() {
        Course course = new Course();

        assertNull(course.getTeamId());
    }

    @Test
    void getTeamId_shouldReturnTeamId() {
        Course course = new Course();
        Team team = new Team();
        team.setId(10L);
        course.setTeam(team);

        assertEquals(10L, course.getTeamId());
        assertSame(team, course.getTeam());
    }

    @Test
    void onCreate_shouldSetCreatedAt() {
        Course course = new Course();

        course.onCreate();

        assertNotNull(course.getCreatedAt());
    }

    @Test
    void setters_shouldExposeValues() {
        Course course = new Course();
        course.setId(2L);
        course.setTitle("Curso");
        course.setDescription("Descricao");

        assertEquals(2L, course.getId());
        assertEquals("Curso", course.getTitle());
        assertEquals("Descricao", course.getDescription());
    }
}
