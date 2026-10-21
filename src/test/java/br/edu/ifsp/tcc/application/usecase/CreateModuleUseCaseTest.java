package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateModuleDTO;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
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
class CreateModuleUseCaseTest {

    @Mock private CurriculumService curriculumService;
    @InjectMocks private CreateModuleUseCase useCase;

    private CreateModuleDTO dto(String title) {
        CreateModuleDTO dto = new CreateModuleDTO();
        dto.setTitle(title);
        return dto;
    }

    @Test
    void execute_shouldVerifyOwnershipAndSave() {
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        when(curriculumService.verifyOwnership(1L, 1L)).thenReturn(item);
        when(curriculumService.countModulesByKanbanItemId(1L)).thenReturn(2);
        when(curriculumService.saveModule(any(Module.class))).thenAnswer(inv -> inv.getArgument(0));

        Module result = useCase.execute(1L, 1L, dto("Novo Modulo"));

        assertEquals("Novo Modulo", result.getTitle());
        assertEquals(2, result.getSortOrder());
        assertEquals(item, result.getKanbanItem());
    }

    @Test
    void execute_shouldThrowWhenNotOwner() {
        when(curriculumService.verifyOwnership(1L, 999L)).thenThrow(new RuntimeException("Item nao encontrado."));

        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 999L, dto("Modulo")));
        verify(curriculumService, never()).saveModule(any());
    }
}
