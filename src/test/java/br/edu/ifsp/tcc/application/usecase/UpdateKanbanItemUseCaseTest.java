package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.UpdateKanbanItemDTO;
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
class UpdateKanbanItemUseCaseTest {

    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private UpdateKanbanItemUseCase useCase;

    private KanbanItem existingItem() {
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        item.setTitle("Original");
        item.setDescription("Original desc");
        item.setState(KanbanItemState.IDEATION);
        item.setProgress(0);
        return item;
    }

    @Test
    void execute_shouldUpdateOnlyProvidedFields() {
        KanbanItem item = existingItem();
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(item));
        when(kanbanItemService.save(item)).thenReturn(item);

        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setTitle("Updated Title");
        dto.setProgress(50);

        KanbanItem result = useCase.execute(1L, 1L, dto);

        assertEquals("Updated Title", result.getTitle());
        assertEquals("Original desc", result.getDescription());
        assertEquals(50, result.getProgress());
        assertEquals(KanbanItemState.IDEATION, result.getState());
    }

    @Test
    void execute_shouldUpdateState() {
        KanbanItem item = existingItem();
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(item));
        when(kanbanItemService.save(item)).thenReturn(item);

        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setState(KanbanItemState.DONE);

        KanbanItem result = useCase.execute(1L, 1L, dto);

        assertEquals(KanbanItemState.DONE, result.getState());
    }

    @Test
    void execute_shouldThrowWhenItemNotFound() {
        when(kanbanItemService.findByIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setTitle("x");

        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 99L, dto));
    }
}
