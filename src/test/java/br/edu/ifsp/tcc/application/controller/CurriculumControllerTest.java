package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.entity.Lesson;
import br.edu.ifsp.tcc.application.entity.Module;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculumControllerTest {

    @Mock private ListModulesUseCase listModulesUseCase;
    @Mock private CreateModuleUseCase createModuleUseCase;
    @Mock private UpdateModuleUseCase updateModuleUseCase;
    @Mock private DeleteModuleUseCase deleteModuleUseCase;
    @Mock private CreateLessonUseCase createLessonUseCase;
    @Mock private UpdateLessonUseCase updateLessonUseCase;
    @Mock private DeleteLessonUseCase deleteLessonUseCase;
    @InjectMocks private CurriculumController controller;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
    }

    @Test
    void createModule_shouldReturnCreatedModule() {
        CreateModuleDTO dto = new CreateModuleDTO();
        Module module = new Module();
        when(createModuleUseCase.execute(5L, 1L, dto)).thenReturn(module);

        ResponseEntity<Module> response = controller.createModule(user, 5L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(module, response.getBody());
    }

    @Test
    void listModules_shouldReturnModules() {
        List<Module> modules = List.of(new Module());
        when(listModulesUseCase.execute(5L, 1L)).thenReturn(modules);

        ResponseEntity<List<Module>> response = controller.listModules(user, 5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(modules, response.getBody());
    }

    @Test
    void updateModule_shouldReturnUpdatedModule() {
        UpdateModuleDTO dto = new UpdateModuleDTO();
        Module module = new Module();
        when(updateModuleUseCase.execute(5L, 1L, 2L, dto)).thenReturn(module);

        ResponseEntity<Module> response = controller.updateModule(user, 5L, 2L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(module, response.getBody());
    }

    @Test
    void deleteModule_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.deleteModule(user, 5L, 2L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteModuleUseCase).execute(5L, 1L, 2L);
    }

    @Test
    void createLesson_shouldReturnCreatedLesson() {
        CreateLessonDTO dto = new CreateLessonDTO();
        Lesson lesson = new Lesson();
        when(createLessonUseCase.execute(5L, 1L, 2L, dto)).thenReturn(lesson);

        ResponseEntity<Lesson> response = controller.createLesson(user, 5L, 2L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(lesson, response.getBody());
    }

    @Test
    void updateLesson_shouldReturnUpdatedLesson() {
        UpdateLessonDTO dto = new UpdateLessonDTO();
        Lesson lesson = new Lesson();
        when(updateLessonUseCase.execute(5L, 1L, 2L, 3L, dto)).thenReturn(lesson);

        ResponseEntity<Lesson> response = controller.updateLesson(user, 5L, 2L, 3L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(lesson, response.getBody());
    }

    @Test
    void deleteLesson_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.deleteLesson(user, 5L, 2L, 3L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteLessonUseCase).execute(5L, 1L, 2L, 3L);
    }
}
