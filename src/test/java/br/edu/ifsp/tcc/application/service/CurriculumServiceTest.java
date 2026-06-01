package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.Lesson;
import br.edu.ifsp.tcc.application.entity.Module;
import br.edu.ifsp.tcc.application.repository.LessonRepository;
import br.edu.ifsp.tcc.application.repository.ModuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculumServiceTest {

    @Mock private ModuleRepository moduleRepository;
    @Mock private LessonRepository lessonRepository;
    @InjectMocks private CurriculumService curriculumService;

    @Test
    void saveModule_shouldDelegate() {
        Module module = new Module();
        when(moduleRepository.save(module)).thenReturn(module);
        assertEquals(module, curriculumService.saveModule(module));
    }

    @Test
    void findModulesByKanbanItemId_shouldReturnOrdered() {
        List<Module> modules = List.of(new Module());
        when(moduleRepository.findByKanbanItemIdOrderBySortOrderAsc(1L)).thenReturn(modules);
        assertEquals(1, curriculumService.findModulesByKanbanItemId(1L).size());
    }

    @Test
    void findModuleByIdAndKanbanItemId_shouldReturn() {
        Module module = new Module();
        when(moduleRepository.findByIdAndKanbanItemId(1L, 1L)).thenReturn(Optional.of(module));
        assertTrue(curriculumService.findModuleByIdAndKanbanItemId(1L, 1L).isPresent());
    }

    @Test
    void countModulesByKanbanItemId_shouldReturn() {
        when(moduleRepository.countByKanbanItemId(1L)).thenReturn(3);
        assertEquals(3, curriculumService.countModulesByKanbanItemId(1L));
    }

    @Test
    void deleteModule_shouldDelegate() {
        curriculumService.deleteModule(1L);
        verify(moduleRepository).deleteById(1L);
    }

    @Test
    void saveLesson_shouldDelegate() {
        Lesson lesson = new Lesson();
        when(lessonRepository.save(lesson)).thenReturn(lesson);
        assertEquals(lesson, curriculumService.saveLesson(lesson));
    }

    @Test
    void findLessonByIdAndModuleId_shouldReturn() {
        Lesson lesson = new Lesson();
        when(lessonRepository.findByIdAndModuleId(1L, 1L)).thenReturn(Optional.of(lesson));
        assertTrue(curriculumService.findLessonByIdAndModuleId(1L, 1L).isPresent());
    }

    @Test
    void countLessonsByModuleId_shouldReturn() {
        when(lessonRepository.countByModuleId(1L)).thenReturn(5);
        assertEquals(5, curriculumService.countLessonsByModuleId(1L));
    }

    @Test
    void deleteLesson_shouldDelegate() {
        curriculumService.deleteLesson(1L);
        verify(lessonRepository).deleteById(1L);
    }
}
