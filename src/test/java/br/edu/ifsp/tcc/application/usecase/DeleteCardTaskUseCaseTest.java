package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.CardTask;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteCardTaskUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @InjectMocks private DeleteCardTaskUseCase useCase;

    @Test
    void execute_shouldDeleteTaskAndRefreshProgress() {
        KanbanItem item = new KanbanItem();
        CardTask task = new CardTask();
        when(boardService.findCard(10L, 3L)).thenReturn(item);
        when(boardService.findTask(3L, 4L)).thenReturn(task);

        useCase.execute(10L, 1L, 3L, 4L);

        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
        verify(boardService).deleteTask(task);
        verify(boardService).refreshProgress(item);
    }
}
