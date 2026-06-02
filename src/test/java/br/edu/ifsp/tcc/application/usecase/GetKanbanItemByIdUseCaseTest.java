package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
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
class GetKanbanItemByIdUseCaseTest {

    @Mock
    private KanbanItemService kanbanItemService;

    @InjectMocks
    private GetKanbanItemByIdUseCase getKanbanItemByIdUseCase;

    @Test
    void execute_shouldReturnItemWhenFoundAndOwnedByUser() {
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        item.setTitle("Meu item");

        when(kanbanItemService.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(item));

        Optional<KanbanItem> result = getKanbanItemByIdUseCase.execute(1L, 10L);

        assertTrue(result.isPresent());
        assertEquals("Meu item", result.get().getTitle());
        verify(kanbanItemService).findByIdAndUserId(1L, 10L);
    }

    @Test
    void execute_shouldReturnEmptyWhenNotFound() {
        when(kanbanItemService.findByIdAndUserId(1L, 10L)).thenReturn(Optional.empty());

        Optional<KanbanItem> result = getKanbanItemByIdUseCase.execute(1L, 10L);

        assertTrue(result.isEmpty());
    }

    @Test
    void execute_shouldReturnEmptyWhenItemBelongsToAnotherUser() {
        when(kanbanItemService.findByIdAndUserId(1L, 999L)).thenReturn(Optional.empty());

        Optional<KanbanItem> result = getKanbanItemByIdUseCase.execute(1L, 999L);

        assertTrue(result.isEmpty());
    }
}
