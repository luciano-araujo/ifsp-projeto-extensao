package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ConvertToProjectDTO;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConvertToProjectUseCaseTest {

    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private ConvertToProjectUseCase useCase;

    private KanbanItem ideationItem() {
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        item.setTitle("Raw Idea");
        item.setState(KanbanItemState.IDEATION);
        return item;
    }

    @Test
    void execute_shouldConvertIdeationItemToProject() {
        KanbanItem item = ideationItem();
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(item));
        when(kanbanItemService.save(item)).thenReturn(item);

        ConvertToProjectDTO dto = new ConvertToProjectDTO();
        dto.setTitle("Curso de Java");
        dto.setTargetAudience("Iniciantes");
        dto.setPedagogicalObjective("Ensinar fundamentos da linguagem Java");

        KanbanItem result = useCase.execute(1L, 1L, dto);

        assertEquals("Curso de Java", result.getTitle());
        assertEquals("Iniciantes", result.getTargetAudience());
        assertEquals("Ensinar fundamentos da linguagem Java", result.getPedagogicalObjective());
        assertEquals(KanbanItemState.IN_PRODUCTION, result.getState());
    }

    @Test
    void execute_shouldThrowWhenItemNotInIdeation() {
        KanbanItem item = ideationItem();
        item.setState(KanbanItemState.IN_PRODUCTION);
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(item));

        ConvertToProjectDTO dto = new ConvertToProjectDTO();
        dto.setTitle("x");
        dto.setTargetAudience("x");
        dto.setPedagogicalObjective("xxxxxxxxxx");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 1L, dto));
        assertTrue(ex.getMessage().contains("ideacao"));
    }

    @Test
    void execute_shouldThrowWhenItemNotFound() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        ConvertToProjectDTO dto = new ConvertToProjectDTO();

        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 1L, dto));
    }
}
