package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.BoardResponse;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.dto.ReviewStatus;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.CardReviewService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.MaterialService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
    @Mock private MaterialService materialService;
    @Mock private CardReviewService cardReviewService;
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
        LocalDateTime since = LocalDateTime.of(2026, 11, 1, 14, 10);
        when(cardReviewService.findStatuses(List.of(inPesquisa, withoutColumn, inRoteiro)))
                .thenReturn(Map.of(4L, ReviewStatus.CHANGES_REQUESTED));
        when(boardService.findColumnSince(List.of(inPesquisa, withoutColumn, inRoteiro))).thenReturn(Map.of(2L, since));

        when(materialService.countByCards(List.of(inPesquisa, withoutColumn, inRoteiro))).thenReturn(Map.of(2L, 3));

        BoardResponse board = useCase.execute(10L, 1L);

        assertEquals("Equipe", board.getTeamName());
        // Projects without a column fall into the first column; the idea is not on the board.
        // columnSince comes from one batch query, null for a card with no movement yet.
        verify(boardService).toCardResponse(inPesquisa, 100L, List.of(), since, 3, ReviewStatus.NONE);
        verify(boardService).toCardResponse(withoutColumn, 100L, List.of(), null, 0, ReviewStatus.NONE);
        verify(boardService).toCardResponse(inRoteiro, 101L, List.of(), null, 0, ReviewStatus.CHANGES_REQUESTED);
        verify(boardService, never()).toCardResponse(eq(idea), any(), any(), any(), anyInt(), any());
        verify(boardService, times(1)).findColumnSince(anyList());
        verify(cardReviewService, times(1)).findStatuses(anyList());
        verify(boardService, times(2)).toColumnResponse(any(BoardColumn.class), anyList());
    }
}
