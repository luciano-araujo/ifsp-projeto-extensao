package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.repository.KanbanItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
    void findCardOfMember_shouldReturnCardWhenUserIsTeamMember() {
        KanbanItem item = createItem();
        when(kanbanItemRepository.findAccessible(1L, 1L)).thenReturn(Optional.of(item));

        assertSame(item, kanbanItemService.findCardOfMember(1L, 1L));
    }

    @Test
    void findCardOfMember_shouldThrow404WhenNotMemberOrMissing() {
        when(kanbanItemRepository.findAccessible(1L, 99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> kanbanItemService.findCardOfMember(1L, 99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Card não encontrado.", ex.getReason());
    }

    @Test
    void deleteById_shouldDelegateToRepository() {
        kanbanItemService.deleteById(1L);

        verify(kanbanItemRepository).deleteById(1L);
    }
}
