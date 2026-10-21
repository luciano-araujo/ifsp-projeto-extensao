package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.UpdateModuleDTO;
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
class UpdateModuleUseCaseTest {

    @Mock private CurriculumService curriculumService;
    @InjectMocks private UpdateModuleUseCase useCase;

    @Test
    void execute_shouldVerifyOwnershipAndUpdate() {
        Module module = new Module();
        module.setId(10L);
        module.setTitle("Modulo Original");
        when(curriculumService.findModule(1L, 10L)).thenReturn(module);
        when(curriculumService.saveModule(any(Module.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateModuleDTO dto = new UpdateModuleDTO();
        dto.setTitle("Titulo Atualizado");
        dto.setSortOrder(5);

        Module result = useCase.execute(1L, 1L, 10L, dto);

        assertEquals("Titulo Atualizado", result.getTitle());
        assertEquals(5, result.getSortOrder());
        verify(curriculumService).verifyOwnership(1L, 1L);
    }

    @Test
    void execute_shouldThrowWhenModuleNotFound() {
        when(curriculumService.findModule(1L, 10L)).thenThrow(new RuntimeException("Modulo nao encontrado."));

        UpdateModuleDTO dto = new UpdateModuleDTO();
        dto.setTitle("X");

        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 1L, 10L, dto));
        verify(curriculumService, never()).saveModule(any());
    }
}
