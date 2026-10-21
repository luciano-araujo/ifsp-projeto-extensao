package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.UpdateLessonDTO;
import br.edu.ifsp.tcc.application.entity.Lesson;
import br.edu.ifsp.tcc.application.entity.LessonType;
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
class UpdateLessonUseCaseTest {

    @Mock private CurriculumService curriculumService;
    @InjectMocks private UpdateLessonUseCase useCase;

    @Test
    void execute_shouldVerifyOwnershipAndUpdate() {
        Lesson lesson = new Lesson();
        lesson.setId(100L);
        lesson.setTitle("Aula Original");
        lesson.setType(LessonType.VIDEO);
        lesson.setPublished(false);
        when(curriculumService.findLesson(10L, 100L)).thenReturn(lesson);
        when(curriculumService.saveLesson(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateLessonDTO dto = new UpdateLessonDTO();
        dto.setTitle("Titulo Atualizado");
        dto.setType(LessonType.QUIZ);
        dto.setPublished(true);
        dto.setSortOrder(7);

        Lesson result = useCase.execute(1L, 1L, 10L, 100L, dto);

        assertEquals("Titulo Atualizado", result.getTitle());
        assertEquals(LessonType.QUIZ, result.getType());
        assertTrue(result.getPublished());
        assertEquals(7, result.getSortOrder());
        verify(curriculumService).verifyOwnership(1L, 1L);
        verify(curriculumService).findModule(1L, 10L);
    }

    @Test
    void execute_shouldThrowWhenLessonNotFound() {
        when(curriculumService.findLesson(10L, 100L)).thenThrow(new RuntimeException("Aula nao encontrada."));

        UpdateLessonDTO dto = new UpdateLessonDTO();
        dto.setTitle("X");

        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 1L, 10L, 100L, dto));
        verify(curriculumService, never()).saveLesson(any());
    }
}
