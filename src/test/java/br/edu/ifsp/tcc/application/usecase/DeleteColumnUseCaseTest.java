package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.BoardColumn;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
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
    }
}
