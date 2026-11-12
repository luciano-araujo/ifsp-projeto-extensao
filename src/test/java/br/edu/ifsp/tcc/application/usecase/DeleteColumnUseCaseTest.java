package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.BoardColumn;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import org.junit.jupiter.api.BeforeEach;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
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
class DeleteColumnUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private DeleteColumnUseCase useCase;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        TeamMember member = new TeamMember();
        member.setUser(admin);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(member);
    }

    private BoardColumn column(Long id) {
        BoardColumn column = new BoardColumn();
        column.setId(id);
        return column;
    }

    @Test
    void execute_shouldMoveCardsToTargetColumn() {
        BoardColumn pesquisa = column(100L);
        BoardColumn roteiro = column(101L);
        KanbanItem inPesquisa = new KanbanItem();
        inPesquisa.setColumn(pesquisa);
        when(boardService.findColumn(10L, 100L)).thenReturn(pesquisa);
        when(boardService.findColumn(10L, 101L)).thenReturn(roteiro);
        when(boardService.findColumns(10L)).thenReturn(List.of(pesquisa, roteiro));
        when(kanbanItemService.findByTeamId(10L)).thenReturn(List.of(inPesquisa));

        useCase.execute(10L, 1L, 100L, 101L);

        assertSame(roteiro, inPesquisa.getColumn());
        verify(kanbanItemService).save(inPesquisa);
        verify(boardService).recordMovement(inPesquisa, roteiro, admin);
        verify(boardService).deleteColumn(pesquisa);
    }

    @Test
    void execute_shouldKeepAtLeastOneColumn() {
        BoardColumn pesquisa = column(100L);
        when(boardService.findColumn(10L, 100L)).thenReturn(pesquisa);
        when(boardService.findColumns(10L)).thenReturn(List.of(pesquisa));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 100L, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(boardService, never()).deleteColumn(any());
        verify(boardService, never()).recordMovement(any(), any(), any());
    }

    @Test
    void execute_shouldMoveCardsToFirstRemainingColumnAndRecordOneMovementPerCard() {
        BoardColumn pesquisa = column(100L);
        BoardColumn roteiro = column(101L);
        KanbanItem first = new KanbanItem();
        first.setColumn(pesquisa);
        KanbanItem second = new KanbanItem();
        second.setColumn(pesquisa);
        KanbanItem elsewhere = new KanbanItem();
        elsewhere.setColumn(roteiro);
        when(boardService.findColumn(10L, 100L)).thenReturn(pesquisa);
        when(boardService.findColumns(10L)).thenReturn(List.of(pesquisa, roteiro));
        when(kanbanItemService.findByTeamId(10L)).thenReturn(List.of(first, second, elsewhere));

        useCase.execute(10L, 1L, 100L, null);

        verify(boardService).recordMovement(first, roteiro, admin);
        verify(boardService).recordMovement(second, roteiro, admin);
        verify(boardService, times(2)).recordMovement(any(), any(), any());
        assertSame(roteiro, elsewhere.getColumn());
    }
}
