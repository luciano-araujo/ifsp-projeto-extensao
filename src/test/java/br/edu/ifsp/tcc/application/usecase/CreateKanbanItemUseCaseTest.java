package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateKanbanItemDTO;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateKanbanItemUseCaseTest {

    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private CreateKanbanItemUseCase createKanbanItemUseCase;

    @Test
    void execute_shouldCreateItemInIdeationState() {
        CreateKanbanItemDTO dto = new CreateKanbanItemDTO();
        dto.setTitle("New Idea");
        dto.setDescription("A description");

        User user = new User();
        user.setId(1L);

        when(kanbanItemService.save(any(KanbanItem.class))).thenAnswer(inv -> {
            KanbanItem item = inv.getArgument(0);
            item.setId(1L);
            return item;
        });

        KanbanItem result = createKanbanItemUseCase.execute(dto, user);

        assertNotNull(result);
        assertEquals("New Idea", result.getTitle());
        assertEquals("A description", result.getDescription());
        assertEquals(KanbanItemState.IDEATION, result.getState());
        assertEquals(user, result.getUser());
    }
}
