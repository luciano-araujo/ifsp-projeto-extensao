package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardTaskResponse;
import br.edu.ifsp.tcc.application.dto.ReorderTasksDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReorderCardTasksUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @InjectMocks private ReorderCardTasksUseCase useCase;

    private CardTask task(Long id, int sortOrder) {
        CardTask task = new CardTask();
        task.setId(id);
        task.setSortOrder(sortOrder);
        return task;
    }

    private ReorderTasksDTO dto(Long... ids) {
        ReorderTasksDTO dto = new ReorderTasksDTO();
        dto.setTaskIds(List.of(ids));
        return dto;
    }

    @Test
    void execute_shouldRewriteSortOrderAndReturnTasksInTheNewOrder() {
        KanbanItem card = new KanbanItem();
        CardTask t31 = task(31L, 0);
        CardTask t32 = task(32L, 1);
        CardTask t33 = task(33L, 2);
        CardTaskResponse r33 = mock(CardTaskResponse.class);
        CardTaskResponse r31 = mock(CardTaskResponse.class);
        CardTaskResponse r32 = mock(CardTaskResponse.class);
        when(boardService.findCard(10L, 14L)).thenReturn(card);
        when(boardService.findTasks(card)).thenReturn(List.of(t31, t32, t33));
        when(boardService.saveTask(any(CardTask.class))).thenAnswer(inv -> inv.getArgument(0));
        when(boardService.toTaskResponse(t33)).thenReturn(r33);
        when(boardService.toTaskResponse(t31)).thenReturn(r31);
        when(boardService.toTaskResponse(t32)).thenReturn(r32);

        List<CardTaskResponse> result = useCase.execute(10L, 1L, 14L, dto(33L, 31L, 32L));

        assertEquals(List.of(r33, r31, r32), result);
        assertEquals(0, t33.getSortOrder());
        assertEquals(1, t31.getSortOrder());
        assertEquals(2, t32.getSortOrder());
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
        verify(boardService).requireExactIds(List.of(33L, 31L, 32L), List.of(31L, 32L, 33L),
                "A lista deve conter exatamente as tarefas do card.");
    }

    @Test
    void execute_shouldRejectListThatDoesNotMatchTheCardTasks() {
        KanbanItem card = new KanbanItem();
        when(boardService.findCard(10L, 14L)).thenReturn(card);
        when(boardService.findTasks(card)).thenReturn(List.of(task(31L, 0), task(32L, 1)));
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "A lista deve conter exatamente as tarefas do card."))
                .when(boardService).requireExactIds(any(), any(), any());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 14L, dto(31L)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(boardService, never()).saveTask(any());
    }

    @Test
    void execute_shouldBeBlockedForReviewer() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN, TeamRole.CREATOR))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe não permite esta ação."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 2L, 14L, dto(1L)));
        verifyNoInteractions(boardService);
    }
}
