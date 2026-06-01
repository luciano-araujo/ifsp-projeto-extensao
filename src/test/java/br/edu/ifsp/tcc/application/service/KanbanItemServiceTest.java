package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.repository.KanbanItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KanbanItemServiceTest {

    @Mock private KanbanItemRepository kanbanItemRepository;
    @InjectMocks private KanbanItemService kanbanItemService;

    private KanbanItem createItem() {
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        item.setTitle("Test Item");
        item.setState(KanbanItemState.IDEATION);
        return item;
    }

    @Test
    void save_shouldDelegateToRepository() {
        KanbanItem item = createItem();
        when(kanbanItemRepository.save(item)).thenReturn(item);

        KanbanItem result = kanbanItemService.save(item);

        assertEquals(item, result);
        verify(kanbanItemRepository).save(item);
    }

    @Test
    void findByUserId_shouldReturnUserItems() {
        List<KanbanItem> items = List.of(createItem());
        when(kanbanItemRepository.findByUser_Id(1L)).thenReturn(items);

        List<KanbanItem> result = kanbanItemService.findByUserId(1L);

        assertEquals(1, result.size());
    }

    @Test
    void findByUserIdAndState_shouldFilterByState() {
        List<KanbanItem> items = List.of(createItem());
        when(kanbanItemRepository.findByUser_IdAndState(1L, KanbanItemState.IDEATION)).thenReturn(items);

        List<KanbanItem> result = kanbanItemService.findByUserIdAndState(1L, KanbanItemState.IDEATION);

        assertEquals(1, result.size());
        assertEquals(KanbanItemState.IDEATION, result.get(0).getState());
    }

    @Test
    void findByIdAndUserId_shouldReturnItemWhenOwned() {
        KanbanItem item = createItem();
        when(kanbanItemRepository.findByIdAndUser_Id(1L, 1L)).thenReturn(Optional.of(item));

        Optional<KanbanItem> result = kanbanItemService.findByIdAndUserId(1L, 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findByIdAndUserId_shouldReturnEmptyWhenNotOwned() {
        when(kanbanItemRepository.findByIdAndUser_Id(1L, 99L)).thenReturn(Optional.empty());

        Optional<KanbanItem> result = kanbanItemService.findByIdAndUserId(1L, 99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteById_shouldDelegateToRepository() {
        kanbanItemService.deleteById(1L);

        verify(kanbanItemRepository).deleteById(1L);
    }
}
