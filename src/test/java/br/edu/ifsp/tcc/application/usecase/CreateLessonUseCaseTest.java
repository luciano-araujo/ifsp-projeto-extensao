package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateLessonDTO;
import br.edu.ifsp.tcc.application.entity.Lesson;
import br.edu.ifsp.tcc.application.entity.LessonType;
import br.edu.ifsp.tcc.application.entity.Module;
import br.edu.ifsp.tcc.application.service.CurriculumService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateLessonUseCaseTest {

    @Mock private CurriculumService curriculumService;
    @InjectMocks private CreateLessonUseCase useCase;

    @Test
    void execute_shouldVerifyOwnershipAndSave() {
        Module module = new Module();
        module.setId(10L);
        when(curriculumService.findModule(1L, 10L)).thenReturn(module);
        when(curriculumService.countLessonsByModuleId(10L)).thenReturn(3);
        when(curriculumService.saveLesson(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateLessonDTO dto = new CreateLessonDTO();
        dto.setTitle("Nova Aula");
        dto.setType(LessonType.TEXT);

        Lesson result = useCase.execute(1L, 1L, 10L, dto);

        assertEquals("Nova Aula", result.getTitle());
        assertEquals(LessonType.TEXT, result.getType());
        assertEquals(3, result.getSortOrder());
        assertEquals(module, result.getModule());
        verify(curriculumService).verifyOwnership(1L, 1L);
    }

    @Test
    void execute_shouldThrowWhenModuleNotFound() {
        when(curriculumService.findModule(1L, 10L)).thenThrow(new RuntimeException("Modulo nao encontrado."));

        CreateLessonDTO dto = new CreateLessonDTO();
        dto.setTitle("Aula");

        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 1L, 10L, dto));
        verify(curriculumService, never()).saveLesson(any());
    }
}
