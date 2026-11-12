package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardTaskResponse;
import br.edu.ifsp.tcc.application.dto.CreateTaskDTO;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCardTaskUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @InjectMocks private CreateCardTaskUseCase useCase;

    @Test
    void execute_shouldAddTaskAndRefreshProgress() {
        CreateTaskDTO dto = new CreateTaskDTO();
        dto.setTitle("  Gravar roteiro ");
        KanbanItem item = new KanbanItem();
        CardTask saved = new CardTask();
        CardTaskResponse response = mock(CardTaskResponse.class);
        when(boardService.findCard(10L, 3L)).thenReturn(item);
        when(boardService.countTasks(3L)).thenReturn(2);
        when(boardService.saveTask(any(CardTask.class))).thenReturn(saved);
        when(boardService.toTaskResponse(saved)).thenReturn(response);

        CardTaskResponse result = useCase.execute(10L, 1L, 3L, dto);

        assertSame(response, result);
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
    }
}
