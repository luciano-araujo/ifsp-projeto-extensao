package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KanbanItemControllerTest {

    @Mock private GetKanbanItemByIdUseCase getKanbanItemByIdUseCase;
    @Mock private UpdateKanbanItemUseCase updateKanbanItemUseCase;
    @Mock private DeleteKanbanItemUseCase deleteKanbanItemUseCase;
    @InjectMocks private KanbanItemController controller;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
    }

    @Test
    void getById_shouldReturnItemWhenFound() {
        KanbanItem item = new KanbanItem();
        when(getKanbanItemByIdUseCase.execute(2L, 1L)).thenReturn(Optional.of(item));

        ResponseEntity<KanbanItem> response = controller.getById(user, 2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(item, response.getBody());
    }

    @Test
    void getById_shouldReturnNotFoundWhenMissing() {
        when(getKanbanItemByIdUseCase.execute(2L, 1L)).thenReturn(Optional.empty());

        ResponseEntity<KanbanItem> response = controller.getById(user, 2L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void update_shouldReturnUpdatedItem() {
        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        KanbanItem item = new KanbanItem();
        when(updateKanbanItemUseCase.execute(2L, 1L, dto)).thenReturn(item);

        ResponseEntity<KanbanItem> response = controller.update(user, 2L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(item, response.getBody());
    }

    @Test
    void delete_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.delete(user, 2L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteKanbanItemUseCase).execute(2L, 1L);
    }
}
