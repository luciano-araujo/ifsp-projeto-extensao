package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LessonMaterialTest {

    @Test
    void getLessonId_shouldBeNullWithoutLessonAndTheLessonIdOtherwise() {
        LessonMaterial link = new LessonMaterial();
        assertNull(link.getLessonId());

        CourseLesson lesson = new CourseLesson();
        lesson.setId(40L);
        link.setLesson(lesson);

        assertEquals(40L, link.getLessonId());
        assertSame(lesson, link.getLesson());
    }

    @Test
    void onCreate_shouldSetCreatedAt() {
        LessonMaterial link = new LessonMaterial();

        link.onCreate();

        assertNotNull(link.getCreatedAt());
    }

    @Test
    void setters_shouldExposeValues() {
        LessonMaterial link = new LessonMaterial();
        Material material = new Material();
        link.setId(3L);
        link.setMaterial(material);

        assertEquals(3L, link.getId());
        assertSame(material, link.getMaterial());
    }
}
