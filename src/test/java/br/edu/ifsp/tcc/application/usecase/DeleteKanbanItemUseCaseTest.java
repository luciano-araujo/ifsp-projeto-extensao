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
class DeleteKanbanItemUseCaseTest {

    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private DeleteKanbanItemUseCase useCase;

    @Test
    void execute_shouldDeleteItemWhenOwnedByUser() {
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(item));

        useCase.execute(1L, 1L);

        verify(kanbanItemService).deleteById(1L);
    }

    @Test
    void execute_shouldThrowWhenItemNotOwnedByUser() {
        when(kanbanItemService.findByIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 99L));
        verify(kanbanItemService, never()).deleteById(anyLong());
    }
}
