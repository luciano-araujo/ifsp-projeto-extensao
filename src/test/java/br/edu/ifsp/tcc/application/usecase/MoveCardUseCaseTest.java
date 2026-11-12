package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.MoveCardDTO;
import br.edu.ifsp.tcc.application.entity.BoardColumn;
import br.edu.ifsp.tcc.application.entity.CardMovement;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoveCardUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private MoveCardUseCase useCase;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        TeamMember member = new TeamMember();
        member.setUser(user);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR)).thenReturn(member);
    }

    private MoveCardDTO dto(Long columnId) {
        MoveCardDTO dto = new MoveCardDTO();
        dto.setColumnId(columnId);
        dto.setSortOrder(0);
        return dto;
    }

    private KanbanItem card(KanbanItemState state) {
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        item.setState(state);
        return item;
    }

    private BoardColumn column(Long id) {
        BoardColumn column = new BoardColumn();
        column.setId(id);
        return column;
    }

    @Test
    void execute_shouldRejectIdeaStillInInbox() {
        when(boardService.findCard(10L, 1L)).thenReturn(card(KanbanItemState.IDEATION));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 1L, dto(101L)));
        verify(kanbanItemService, never()).save(any());
        verify(boardService, never()).recordMovement(any(), any(), any());
    }

    @Test
    void execute_shouldRejectColumnFromAnotherTeam() {
        KanbanItem project = card(KanbanItemState.IN_PRODUCTION);
        when(boardService.findCard(10L, 1L)).thenReturn(project);
        when(boardService.findColumn(10L, 999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Coluna nao encontrada."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 1L, dto(999L)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertNull(project.getColumn());
    }

    @Test
    void execute_shouldChangeColumnAndRecordTheMovement() {
        KanbanItem project = card(KanbanItemState.IN_PRODUCTION);
        project.setColumn(column(100L));
        BoardColumn roteiro = column(101L);
        LocalDateTime enteredAt = LocalDateTime.of(2026, 11, 1, 14, 10);
        CardMovement movement = new CardMovement();
        movement.setEnteredAt(enteredAt);
        when(boardService.findCard(10L, 1L)).thenReturn(project);
        when(boardService.findColumn(10L, 101L)).thenReturn(roteiro);
        when(kanbanItemService.save(project)).thenReturn(project);
        when(boardService.recordMovement(project, roteiro, user)).thenReturn(movement);
        when(boardService.findTasks(project)).thenReturn(List.of());

        useCase.execute(10L, 1L, 1L, dto(101L));

        assertSame(roteiro, project.getColumn());
        assertEquals(0, project.getSortOrder());
        verify(boardService).recordMovement(project, roteiro, user);
        verify(boardService).toCardResponse(project, 101L, List.of(), enteredAt);
    }

    @Test
    void execute_shouldNotRecordMovementWhenProjectWithoutColumnIsMovedToTheFirstColumn() {
        KanbanItem project = card(KanbanItemState.IN_PRODUCTION);
        BoardColumn pesquisa = column(100L);
        LocalDateTime since = LocalDateTime.of(2026, 10, 28, 18, 0);
        when(boardService.findCard(10L, 1L)).thenReturn(project);
        when(boardService.findColumn(10L, 100L)).thenReturn(pesquisa);
        when(boardService.findColumns(10L)).thenReturn(List.of(pesquisa, column(101L)));
        when(kanbanItemService.save(project)).thenReturn(project);
        when(boardService.findColumnSince(List.of(project))).thenReturn(Map.of(1L, since));

        useCase.execute(10L, 1L, 1L, dto(100L));

        verify(boardService, never()).recordMovement(any(), any(), any());
        verify(boardService).toCardResponse(project, 100L, List.of(), since);
    }

    @Test
    void execute_shouldRecordMovementForProjectWithoutColumnMovedToAnotherColumn() {
        KanbanItem project = card(KanbanItemState.IN_PRODUCTION);
        BoardColumn pesquisa = column(100L);
        BoardColumn roteiro = column(101L);
        when(boardService.findCard(10L, 1L)).thenReturn(project);
        when(boardService.findColumn(10L, 101L)).thenReturn(roteiro);
        when(boardService.findColumns(10L)).thenReturn(List.of(pesquisa, roteiro));
        when(kanbanItemService.save(project)).thenReturn(project);
        when(boardService.recordMovement(project, roteiro, user)).thenReturn(new CardMovement());

        useCase.execute(10L, 1L, 1L, dto(101L));

        verify(boardService).recordMovement(project, roteiro, user);
    }

    @Test
    void execute_shouldRecordMovementForProjectWithoutColumnWhenTheBoardHasNoColumns() {
        KanbanItem project = card(KanbanItemState.IN_PRODUCTION);
        BoardColumn pesquisa = column(100L);
        when(boardService.findCard(10L, 1L)).thenReturn(project);
        when(boardService.findColumn(10L, 100L)).thenReturn(pesquisa);
        when(boardService.findColumns(10L)).thenReturn(List.of());
        when(kanbanItemService.save(project)).thenReturn(project);
        when(boardService.recordMovement(project, pesquisa, user)).thenReturn(new CardMovement());

        useCase.execute(10L, 1L, 1L, dto(100L));

        verify(boardService).recordMovement(project, pesquisa, user);
    }

    @Test
    void execute_shouldNotRecordMovementWhenOnlyReorderingInsideTheSameColumn() {
        KanbanItem project = card(KanbanItemState.IN_PRODUCTION);
        BoardColumn roteiro = column(101L);
        project.setColumn(roteiro);
        LocalDateTime since = LocalDateTime.of(2026, 10, 28, 18, 0);
        when(boardService.findCard(10L, 1L)).thenReturn(project);
        when(boardService.findColumn(10L, 101L)).thenReturn(roteiro);
        when(kanbanItemService.save(project)).thenReturn(project);
        when(boardService.findColumnSince(List.of(project))).thenReturn(Map.of(1L, since));
        when(boardService.findTasks(project)).thenReturn(List.of());

        useCase.execute(10L, 1L, 1L, dto(101L));

        verify(boardService, never()).recordMovement(any(), any(), any());
        verify(boardService).toCardResponse(project, 101L, List.of(), since);
    }
}
