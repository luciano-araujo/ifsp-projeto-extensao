package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.MaterialService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteCardUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private KanbanItemService kanbanItemService;
    @Mock private MaterialService materialService;
    @InjectMocks private DeleteCardUseCase useCase;

    @Test
    void execute_shouldDeleteCardOfTeam() {
        KanbanItem item = new KanbanItem();
        item.setId(3L);
        when(boardService.findCard(10L, 3L)).thenReturn(item);

        useCase.execute(10L, 1L, 3L);

        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
        verify(materialService).scheduleFileRemovalOfCard(3L);
        verify(kanbanItemService).deleteById(3L);
    }
}
