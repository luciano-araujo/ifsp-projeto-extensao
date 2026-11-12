package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.BoardColumnResponse;
import br.edu.ifsp.tcc.application.dto.ReorderColumnsDTO;
import br.edu.ifsp.tcc.application.entity.BoardColumn;
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
class ReorderColumnsUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @InjectMocks private ReorderColumnsUseCase useCase;

    private BoardColumn column(Long id, int sortOrder) {
        BoardColumn column = new BoardColumn();
        column.setId(id);
        column.setSortOrder(sortOrder);
        return column;
    }

    private ReorderColumnsDTO dto(Long... ids) {
        ReorderColumnsDTO dto = new ReorderColumnsDTO();
        dto.setColumnIds(List.of(ids));
        return dto;
    }

    @Test
    void execute_shouldRewriteSortOrderAndReturnColumnsWithoutCards() {
        BoardColumn a = column(4L, 0);
        BoardColumn b = column(5L, 1);
        BoardColumn c = column(6L, 2);
        BoardColumnResponse rc = mock(BoardColumnResponse.class);
        BoardColumnResponse ra = mock(BoardColumnResponse.class);
        BoardColumnResponse rb = mock(BoardColumnResponse.class);
        when(boardService.findColumns(10L)).thenReturn(List.of(a, b, c));
        when(boardService.saveColumn(any(BoardColumn.class))).thenAnswer(inv -> inv.getArgument(0));
        when(boardService.toColumnResponse(c, List.of())).thenReturn(rc);
        when(boardService.toColumnResponse(a, List.of())).thenReturn(ra);
        when(boardService.toColumnResponse(b, List.of())).thenReturn(rb);

        List<BoardColumnResponse> result = useCase.execute(10L, 1L, dto(6L, 4L, 5L));

        assertEquals(List.of(rc, ra, rb), result);
        assertEquals(0, c.getSortOrder());
        assertEquals(1, a.getSortOrder());
        assertEquals(2, b.getSortOrder());
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN);
        verify(boardService).requireExactIds(List.of(6L, 4L, 5L), List.of(4L, 5L, 6L),
                "A lista deve conter exatamente as colunas da equipe.");
    }

    @Test
    void execute_shouldRejectListThatDoesNotMatchTheTeamColumns() {
        when(boardService.findColumns(10L)).thenReturn(List.of(column(4L, 0), column(5L, 1)));
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "A lista deve conter exatamente as colunas da equipe."))
                .when(boardService).requireExactIds(any(), any(), any());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, dto(4L)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(boardService, never()).saveColumn(any());
    }

    @Test
    void execute_shouldBeBlockedForNonAdmin() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe não permite esta ação."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 2L, dto(1L)));
        verifyNoInteractions(boardService);
    }
}
