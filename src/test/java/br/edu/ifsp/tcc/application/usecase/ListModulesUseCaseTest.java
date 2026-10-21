package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Module;
import br.edu.ifsp.tcc.application.service.CurriculumService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListModulesUseCaseTest {

    @Mock private CurriculumService curriculumService;
    @InjectMocks private ListModulesUseCase useCase;

    @Test
    void execute_shouldVerifyOwnershipAndReturn() {
        when(curriculumService.verifyOwnership(1L, 1L)).thenReturn(new KanbanItem());
        when(curriculumService.findModulesByKanbanItemId(1L)).thenReturn(List.of(new Module()));

        assertEquals(1, useCase.execute(1L, 1L).size());
    }

    @Test
    void execute_shouldThrowWhenOwnershipFails() {
        when(curriculumService.verifyOwnership(1L, 999L)).thenThrow(new RuntimeException("Item nao encontrado."));

        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 999L));
        verify(curriculumService, never()).findModulesByKanbanItemId(anyLong());
    }
}
