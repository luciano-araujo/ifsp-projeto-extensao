package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CourseModuleTest {

    @Test
    void getCourseId_shouldReturnNullWithoutCourse() {
        CourseModule module = new CourseModule();

        assertNull(module.getCourseId());
    }

    @Test
    void getCourseId_shouldReturnCourseId() {
        CourseModule module = new CourseModule();
        Course course = new Course();
        course.setId(2L);
        module.setCourse(course);

        assertEquals(2L, module.getCourseId());
        assertSame(course, module.getCourse());
    }

    @Test
    void onCreate_shouldSetCreatedAt() {
        CourseModule module = new CourseModule();

        module.onCreate();

        assertNotNull(module.getCreatedAt());
    }

    @Test
    void setters_shouldExposeValues() {
        CourseModule module = new CourseModule();
        module.setId(3L);
        module.setTitle("Modulo 1");
        module.setSortOrder(1);

        assertEquals(3L, module.getId());
        assertEquals("Modulo 1", module.getTitle());
        assertEquals(1, module.getSortOrder());
    }
}
