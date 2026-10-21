package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.Lesson;
import br.edu.ifsp.tcc.application.service.CurriculumService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteLessonUseCaseTest {

    @Mock private CurriculumService curriculumService;
    @InjectMocks private DeleteLessonUseCase useCase;

    @Test
    void execute_shouldVerifyOwnershipAndDelete() {
        when(curriculumService.findLesson(10L, 100L)).thenReturn(new Lesson());

        useCase.execute(1L, 1L, 10L, 100L);

        verify(curriculumService).verifyOwnership(1L, 1L);
        verify(curriculumService).findModule(1L, 10L);
        verify(curriculumService).deleteLesson(100L);
    }

    @Test
    void execute_shouldThrowWhenLessonNotFound() {
        when(curriculumService.findLesson(10L, 100L)).thenThrow(new RuntimeException("Aula nao encontrada."));

        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 1L, 10L, 100L));
        verify(curriculumService, never()).deleteLesson(anyLong());
    }
}
