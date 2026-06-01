package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LessonTest {

    @Test
    void getModuleId_shouldReturnIdWhenSet() {
        Module module = new Module();
        module.setId(3L);
        Lesson lesson = new Lesson();
        lesson.setModule(module);
        assertEquals(3L, lesson.getModuleId());
    }

    @Test
    void getModuleId_shouldReturnNullWhenNotSet() {
        Lesson lesson = new Lesson();
        assertNull(lesson.getModuleId());
    }

    @Test
    void defaults_shouldBeCorrect() {
        Lesson lesson = new Lesson();
        assertEquals(LessonType.VIDEO, lesson.getType());
        assertFalse(lesson.getPublished());
        assertEquals(0, lesson.getSortOrder());
    }

    @Test
    void settersAndGetters_shouldWork() {
        Lesson lesson = new Lesson();
        lesson.setId(1L);
        lesson.setTitle("Aula 1");
        lesson.setType(LessonType.QUIZ);
        lesson.setPublished(true);
        lesson.setSortOrder(5);

        assertEquals(1L, lesson.getId());
        assertEquals("Aula 1", lesson.getTitle());
        assertEquals(LessonType.QUIZ, lesson.getType());
        assertTrue(lesson.getPublished());
        assertEquals(5, lesson.getSortOrder());
    }
}
