package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetKanbanItemsByUserUseCaseTest {

    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private GetKanbanItemsByUserUseCase useCase;

    @Test
    void execute_shouldReturnAllItemsWhenStateIsNull() {
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        when(kanbanItemService.findByUserId(1L)).thenReturn(List.of(item));

        List<KanbanItem> result = useCase.execute(1L, null);

        assertEquals(1, result.size());
        verify(kanbanItemService).findByUserId(1L);
        verify(kanbanItemService, never()).findByUserIdAndState(anyLong(), any());
    }

    @Test
    void execute_shouldFilterByStateWhenProvided() {
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        item.setState(KanbanItemState.REVIEW);
        when(kanbanItemService.findByUserIdAndState(1L, KanbanItemState.REVIEW)).thenReturn(List.of(item));

        List<KanbanItem> result = useCase.execute(1L, KanbanItemState.REVIEW);

        assertEquals(1, result.size());
        verify(kanbanItemService).findByUserIdAndState(1L, KanbanItemState.REVIEW);
        verify(kanbanItemService, never()).findByUserId(anyLong());
    }
}
