package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.Module;
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
class DeleteModuleUseCaseTest {

    @Mock private CurriculumService curriculumService;
    @InjectMocks private DeleteModuleUseCase useCase;

    @Test
    void execute_shouldVerifyOwnershipAndDelete() {
        when(curriculumService.findModule(1L, 10L)).thenReturn(new Module());

        useCase.execute(1L, 1L, 10L);

        verify(curriculumService).verifyOwnership(1L, 1L);
        verify(curriculumService).deleteModule(10L);
    }

    @Test
    void execute_shouldThrowWhenModuleNotFound() {
        when(curriculumService.findModule(1L, 10L)).thenThrow(new RuntimeException("Modulo nao encontrado."));

        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 1L, 10L));
        verify(curriculumService, never()).deleteModule(anyLong());
    }
}
