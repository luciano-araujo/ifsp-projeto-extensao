package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CreateLessonDTO;
import br.edu.ifsp.tcc.application.dto.CreateModuleDTO;
import br.edu.ifsp.tcc.application.dto.UpdateLessonDTO;
import br.edu.ifsp.tcc.application.dto.UpdateModuleDTO;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.entity.Module;
import br.edu.ifsp.tcc.application.repository.LessonRepository;
import br.edu.ifsp.tcc.application.repository.ModuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculumServiceTest {

    @Mock private ModuleRepository moduleRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private CurriculumService curriculumService;

    private KanbanItem mockKanbanItem() {
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        return item;
    }

    private Module mockModule() {
        Module module = new Module();
        module.setId(10L);
        module.setTitle("Modulo Original");
        return module;
    }

    private Lesson mockLesson() {
        Lesson lesson = new Lesson();
        lesson.setId(100L);
        lesson.setTitle("Aula Original");
        lesson.setType(LessonType.VIDEO);
        lesson.setPublished(false);
        return lesson;
    }

    // =====================================================
    // Ownership verification
    // =====================================================

    @Test
    void anyOperation_shouldThrowWhenOwnershipFails() {
        when(kanbanItemService.findByIdAndUserId(1L, 999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                curriculumService.listModules(1L, 999L));
    }

    // =====================================================
    // Module operations
    // =====================================================

    @Test
    void createModule_shouldVerifyOwnershipAndSave() {
        KanbanItem item = mockKanbanItem();
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(item));
        when(moduleRepository.countByKanbanItem_Id(1L)).thenReturn(2);
        when(moduleRepository.save(any(Module.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateModuleDTO dto = new CreateModuleDTO();
        dto.setTitle("Novo Modulo");

        Module result = curriculumService.createModule(1L, 1L, dto);

        assertEquals("Novo Modulo", result.getTitle());
        assertEquals(2, result.getSortOrder());
        assertEquals(item, result.getKanbanItem());
        verify(kanbanItemService).findByIdAndUserId(1L, 1L);
    }

    @Test
    void createModule_shouldThrowWhenNotOwner() {
        when(kanbanItemService.findByIdAndUserId(1L, 999L)).thenReturn(Optional.empty());

        CreateModuleDTO dto = new CreateModuleDTO();
        dto.setTitle("Modulo");

        assertThrows(RuntimeException.class, () ->
                curriculumService.createModule(1L, 999L, dto));
        verify(moduleRepository, never()).save(any());
    }

    @Test
    void listModules_shouldVerifyOwnershipAndReturn() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockKanbanItem()));
        List<Module> modules = List.of(new Module());
        when(moduleRepository.findByKanbanItemIdOrderBySortOrderAsc(1L)).thenReturn(modules);

        assertEquals(1, curriculumService.listModules(1L, 1L).size());
    }

    @Test
    void updateModule_shouldVerifyOwnershipAndUpdate() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockKanbanItem()));
        Module module = mockModule();
        when(moduleRepository.findByIdAndKanbanItemId(10L, 1L)).thenReturn(Optional.of(module));
        when(moduleRepository.save(any(Module.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateModuleDTO dto = new UpdateModuleDTO();
        dto.setTitle("Titulo Atualizado");
        dto.setSortOrder(5);

        Module result = curriculumService.updateModule(1L, 1L, 10L, dto);

        assertEquals("Titulo Atualizado", result.getTitle());
        assertEquals(5, result.getSortOrder());
    }

    @Test
    void updateModule_shouldThrowWhenModuleNotFound() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockKanbanItem()));
        when(moduleRepository.findByIdAndKanbanItemId(10L, 1L)).thenReturn(Optional.empty());

        UpdateModuleDTO dto = new UpdateModuleDTO();
        dto.setTitle("X");

        assertThrows(RuntimeException.class, () ->
                curriculumService.updateModule(1L, 1L, 10L, dto));
    }

    @Test
    void deleteModule_shouldVerifyOwnershipAndDelete() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockKanbanItem()));
        when(moduleRepository.findByIdAndKanbanItemId(10L, 1L)).thenReturn(Optional.of(mockModule()));

        curriculumService.deleteModule(1L, 1L, 10L);

        verify(moduleRepository).deleteById(10L);
    }

    @Test
    void deleteModule_shouldThrowWhenModuleNotFound() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockKanbanItem()));
        when(moduleRepository.findByIdAndKanbanItemId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                curriculumService.deleteModule(1L, 1L, 10L));
        verify(moduleRepository, never()).deleteById(any());
    }

    // =====================================================
    // Lesson operations
    // =====================================================

    @Test
    void createLesson_shouldVerifyOwnershipAndSave() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockKanbanItem()));
        Module module = mockModule();
        when(moduleRepository.findByIdAndKanbanItemId(10L, 1L)).thenReturn(Optional.of(module));
        when(lessonRepository.countByModule_Id(10L)).thenReturn(3);
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateLessonDTO dto = new CreateLessonDTO();
        dto.setTitle("Nova Aula");
        dto.setType(LessonType.TEXT);

        Lesson result = curriculumService.createLesson(1L, 1L, 10L, dto);

        assertEquals("Nova Aula", result.getTitle());
        assertEquals(LessonType.TEXT, result.getType());
        assertEquals(3, result.getSortOrder());
        assertEquals(module, result.getModule());
    }

    @Test
    void createLesson_shouldThrowWhenModuleNotFound() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockKanbanItem()));
        when(moduleRepository.findByIdAndKanbanItemId(10L, 1L)).thenReturn(Optional.empty());

        CreateLessonDTO dto = new CreateLessonDTO();
        dto.setTitle("Aula");

        assertThrows(RuntimeException.class, () ->
                curriculumService.createLesson(1L, 1L, 10L, dto));
        verify(lessonRepository, never()).save(any());
    }

    @Test
    void updateLesson_shouldVerifyOwnershipAndUpdate() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockKanbanItem()));
        when(moduleRepository.findByIdAndKanbanItemId(10L, 1L)).thenReturn(Optional.of(mockModule()));
        Lesson lesson = mockLesson();
        when(lessonRepository.findByIdAndModule_Id(100L, 10L)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateLessonDTO dto = new UpdateLessonDTO();
        dto.setTitle("Titulo Atualizado");
        dto.setType(LessonType.QUIZ);
        dto.setPublished(true);
        dto.setSortOrder(7);

        Lesson result = curriculumService.updateLesson(1L, 1L, 10L, 100L, dto);

        assertEquals("Titulo Atualizado", result.getTitle());
        assertEquals(LessonType.QUIZ, result.getType());
        assertTrue(result.getPublished());
        assertEquals(7, result.getSortOrder());
    }

    @Test
    void updateLesson_shouldThrowWhenLessonNotFound() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockKanbanItem()));
        when(moduleRepository.findByIdAndKanbanItemId(10L, 1L)).thenReturn(Optional.of(mockModule()));
        when(lessonRepository.findByIdAndModule_Id(100L, 10L)).thenReturn(Optional.empty());

        UpdateLessonDTO dto = new UpdateLessonDTO();
        dto.setTitle("X");

        assertThrows(RuntimeException.class, () ->
                curriculumService.updateLesson(1L, 1L, 10L, 100L, dto));
    }

    @Test
    void deleteLesson_shouldVerifyOwnershipAndDelete() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockKanbanItem()));
        when(moduleRepository.findByIdAndKanbanItemId(10L, 1L)).thenReturn(Optional.of(mockModule()));
        when(lessonRepository.findByIdAndModule_Id(100L, 10L)).thenReturn(Optional.of(mockLesson()));

        curriculumService.deleteLesson(1L, 1L, 10L, 100L);

        verify(lessonRepository).deleteById(100L);
    }

    @Test
    void deleteLesson_shouldThrowWhenLessonNotFound() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockKanbanItem()));
        when(moduleRepository.findByIdAndKanbanItemId(10L, 1L)).thenReturn(Optional.of(mockModule()));
        when(lessonRepository.findByIdAndModule_Id(100L, 10L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                curriculumService.deleteLesson(1L, 1L, 10L, 100L));
        verify(lessonRepository, never()).deleteById(any());
    }

    // =====================================================
    // Low-level accessors (kept for backward compatibility)
    // =====================================================

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
        when(moduleRepository.countByKanbanItem_Id(1L)).thenReturn(3);
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
        when(lessonRepository.findByIdAndModule_Id(1L, 1L)).thenReturn(Optional.of(lesson));
        assertTrue(curriculumService.findLessonByIdAndModuleId(1L, 1L).isPresent());
    }

    @Test
    void countLessonsByModuleId_shouldReturn() {
        when(lessonRepository.countByModule_Id(1L)).thenReturn(5);
        assertEquals(5, curriculumService.countLessonsByModuleId(1L));
    }

    @Test
    void deleteLesson_shouldDelegate() {
        curriculumService.deleteLesson(1L);
        verify(lessonRepository).deleteById(1L);
    }
}
