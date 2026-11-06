package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CourseLessonTest {

    @Test
    void getModuleId_shouldReturnNullWithoutModule() {
        CourseLesson lesson = new CourseLesson();

        assertNull(lesson.getModuleId());
    }

    @Test
    void getModuleId_shouldReturnModuleId() {
        CourseLesson lesson = new CourseLesson();
        CourseModule module = new CourseModule();
        module.setId(3L);
        lesson.setModule(module);

        assertEquals(3L, lesson.getModuleId());
        assertSame(module, lesson.getModule());
    }

    @Test
    void getProjectId_shouldReturnNullWithoutProject() {
        CourseLesson lesson = new CourseLesson();

        assertNull(lesson.getProjectId());
    }

    @Test
    void getProjectId_shouldReturnLinkedProjectId() {
        CourseLesson lesson = new CourseLesson();
        KanbanItem project = new KanbanItem();
        project.setId(7L);
        lesson.setProject(project);

        assertEquals(7L, lesson.getProjectId());
        assertSame(project, lesson.getProject());
    }

    @Test
    void onCreate_shouldSetCreatedAt() {
        CourseLesson lesson = new CourseLesson();

        lesson.onCreate();

        assertNotNull(lesson.getCreatedAt());
    }

    @Test
    void setters_shouldExposeValues() {
        CourseLesson lesson = new CourseLesson();
        lesson.setId(4L);
        lesson.setTitle("Aula 1");
        lesson.setType(LessonType.VIDEO);
        lesson.setPublished(true);
        lesson.setSortOrder(2);

        assertEquals(4L, lesson.getId());
        assertEquals("Aula 1", lesson.getTitle());
        assertEquals(LessonType.VIDEO, lesson.getType());
        assertTrue(lesson.getPublished());
        assertEquals(2, lesson.getSortOrder());
    }
}
