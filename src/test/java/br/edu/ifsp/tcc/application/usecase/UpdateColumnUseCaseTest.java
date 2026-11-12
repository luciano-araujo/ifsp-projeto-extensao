package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.BoardColumnDTO;
import br.edu.ifsp.tcc.application.dto.BoardColumnResponse;
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
class UpdateColumnUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @InjectMocks private UpdateColumnUseCase useCase;

    private BoardColumn column() {
        BoardColumn column = new BoardColumn();
        column.setId(5L);
        column.setName("Roteiro");
        column.setSortOrder(1);
        return column;
    }

    @Test
    void execute_shouldRenameAndReorderColumn() {
        BoardColumn column = column();
        BoardColumnDTO dto = new BoardColumnDTO();
        dto.setName("  Producao ");
        dto.setSortOrder(2);
        BoardColumnResponse response = mock(BoardColumnResponse.class);
        when(boardService.findColumn(10L, 5L)).thenReturn(column);
        when(boardService.saveColumn(column)).thenReturn(column);
        when(boardService.toColumnResponse(column, List.of())).thenReturn(response);

        BoardColumnResponse result = useCase.execute(10L, 1L, 5L, dto);

        assertSame(response, result);
        assertEquals("Producao", column.getName());
        assertEquals(2, column.getSortOrder());
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN);
    }

    @Test
    void execute_shouldRejectBlankNameWhenSent() {
        BoardColumn column = column();
        BoardColumnDTO dto = new BoardColumnDTO();
        dto.setName(" ");
        when(boardService.findColumn(10L, 5L)).thenReturn(column);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 5L, dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("O nome da coluna não pode ficar vazio.", ex.getReason());
        assertEquals("Roteiro", column.getName());
        verify(boardService, never()).saveColumn(any());
    }

    @Test
    void execute_shouldKeepNameAndOrderWhenNotGiven() {
        BoardColumn column = column();
        BoardColumnDTO dto = new BoardColumnDTO();
        when(boardService.findColumn(10L, 5L)).thenReturn(column);
        when(boardService.saveColumn(column)).thenReturn(column);
        when(boardService.toColumnResponse(any(BoardColumn.class), any())).thenReturn(mock(BoardColumnResponse.class));

        useCase.execute(10L, 1L, 5L, dto);

        assertEquals("Roteiro", column.getName());
        assertEquals(1, column.getSortOrder());
    }
}
