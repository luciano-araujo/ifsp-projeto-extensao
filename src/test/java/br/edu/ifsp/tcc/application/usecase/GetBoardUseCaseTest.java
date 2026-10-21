package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.BoardResponse;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetBoardUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private GetBoardUseCase useCase;

    private BoardColumn column(Long id) {
        BoardColumn column = new BoardColumn();
        column.setId(id);
        return column;
    }

    private KanbanItem card(Long id, KanbanItemState state, BoardColumn column) {
        KanbanItem item = new KanbanItem();
        item.setId(id);
        item.setState(state);
        item.setColumn(column);
        return item;
    }

    @Test
    void execute_shouldGroupProjectsByColumnAndLeaveIdeasOut() {
        Team team = new Team();
        team.setId(10L);
        team.setName("Equipe");
        TeamMember member = new TeamMember();
        member.setTeam(team);
        BoardColumn pesquisa = column(100L);
        BoardColumn roteiro = column(101L);
        KanbanItem idea = card(1L, KanbanItemState.IDEATION, null);
        KanbanItem inPesquisa = card(2L, KanbanItemState.IN_PRODUCTION, pesquisa);
        KanbanItem withoutColumn = card(3L, KanbanItemState.IN_PRODUCTION, null);
        KanbanItem inRoteiro = card(4L, KanbanItemState.IN_PRODUCTION, roteiro);

        when(teamService.requireMember(10L, 1L)).thenReturn(member);
        when(boardService.findColumns(10L)).thenReturn(List.of(pesquisa, roteiro));
        when(kanbanItemService.findByTeamId(10L)).thenReturn(List.of(idea, inPesquisa, withoutColumn, inRoteiro));
        when(boardService.findTasksByCard(anyList())).thenReturn(Map.of());

        BoardResponse board = useCase.execute(10L, 1L);

        assertEquals("Equipe", board.getTeamName());
        // Projects without a column fall into the first column; the idea is not on the board
        verify(boardService).toCardResponse(inPesquisa, 100L, List.of());
        verify(boardService).toCardResponse(withoutColumn, 100L, List.of());
        verify(boardService).toCardResponse(inRoteiro, 101L, List.of());
        verify(boardService, never()).toCardResponse(eq(idea), any(), any());
        verify(boardService, times(2)).toColumnResponse(any(BoardColumn.class), anyList());
    }
}
