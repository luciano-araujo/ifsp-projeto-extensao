package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardTaskResponse;
import br.edu.ifsp.tcc.application.dto.UpdateTaskDTO;
import br.edu.ifsp.tcc.application.entity.CardTask;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.TeamMember;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateCardTaskUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @InjectMocks private UpdateCardTaskUseCase useCase;

    private CardTask task;
    private CardTaskResponse response;

    private void givenTask() {
        task = new CardTask();
        task.setTitle("Antigo");
        TeamMember previous = new TeamMember();
        previous.setId(1L);
        task.setAssignee(previous);
        task.setDueDate(LocalDate.of(2026, 11, 1));
        when(boardService.findCard(10L, 3L)).thenReturn(new KanbanItem());
        when(boardService.findTask(3L, 4L)).thenReturn(task);
    }

    private void givenSave() {
        response = mock(CardTaskResponse.class);
        when(boardService.saveTask(task)).thenReturn(task);
        when(boardService.toTaskResponse(task)).thenReturn(response);
    }

    @Test
    void execute_shouldChangeTitleAssigneeAndDueDate() {
        givenTask();
        givenSave();
        TeamMember bia = new TeamMember();
        bia.setId(5L);
        when(teamService.findMemberOfTeam(10L, 5L)).thenReturn(Optional.of(bia));
        UpdateTaskDTO dto = new UpdateTaskDTO();
        dto.setTitle("  Gravar a introdução ");
        dto.setAssigneeMemberId(5L);
        dto.setDueDate("2026-11-05");

        assertSame(response, useCase.execute(10L, 1L, 3L, 4L, dto));

        assertEquals("Gravar a introdução", task.getTitle());
        assertSame(bia, task.getAssignee());
        assertEquals(LocalDate.of(2026, 11, 5), task.getDueDate());
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
    }

    @Test
    void execute_shouldRemoveAssigneeWithZeroAndDueDateWithEmptyString() {
        givenTask();
        givenSave();
        UpdateTaskDTO dto = new UpdateTaskDTO();
        dto.setAssigneeMemberId(0L);
        dto.setDueDate("");

        useCase.execute(10L, 1L, 3L, 4L, dto);

        assertNull(task.getAssignee());
        assertNull(task.getDueDate());
        assertEquals("Antigo", task.getTitle());
        verify(teamService, never()).findMemberOfTeam(any(), any());
    }

    @Test
    void execute_shouldLeaveEverythingAsIsWhenNothingIsSent() {
        givenTask();
        givenSave();

        useCase.execute(10L, 1L, 3L, 4L, new UpdateTaskDTO());

        assertEquals("Antigo", task.getTitle());
        assertNotNull(task.getAssignee());
        assertEquals(LocalDate.of(2026, 11, 1), task.getDueDate());
    }

    @Test
    void execute_shouldRejectAssigneeThatIsNotAMemberOfTheTeam() {
        givenTask();
        when(teamService.findMemberOfTeam(10L, 77L)).thenReturn(Optional.empty());
        UpdateTaskDTO dto = new UpdateTaskDTO();
        dto.setAssigneeMemberId(77L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 3L, 4L, dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(boardService, never()).saveTask(any());
    }

    @Test
    void execute_shouldRejectBlankTitle() {
        givenTask();
        UpdateTaskDTO dto = new UpdateTaskDTO();
        dto.setTitle("   ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 3L, 4L, dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(boardService, never()).saveTask(any());
    }

    @Test
    void execute_shouldRejectImpossibleDate() {
        givenTask();
        UpdateTaskDTO dto = new UpdateTaskDTO();
        dto.setDueDate("2026-13-45");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 3L, 4L, dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(boardService, never()).saveTask(any());
    }

    @Test
    void execute_shouldReturnNotFoundForTaskOfAnotherCard() {
        when(boardService.findCard(10L, 3L)).thenReturn(new KanbanItem());
        when(boardService.findTask(3L, 4L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 3L, 4L, new UpdateTaskDTO()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void execute_shouldBeBlockedForReviewer() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN, TeamRole.CREATOR))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe não permite esta ação."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 2L, 3L, 4L, new UpdateTaskDTO()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(boardService);
    }
}
