package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.MoveCardDTO;
import br.edu.ifsp.tcc.application.entity.BoardColumn;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoveCardUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private MoveCardUseCase useCase;

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

    @Test
    void execute_shouldRejectIdeaStillInInbox() {
        when(boardService.findCard(10L, 1L)).thenReturn(card(KanbanItemState.IDEATION));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 1L, dto(101L)));
        verify(kanbanItemService, never()).save(any());
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
    void execute_shouldChangeColumn() {
        KanbanItem project = card(KanbanItemState.IN_PRODUCTION);
        BoardColumn roteiro = new BoardColumn();
        roteiro.setId(101L);
        when(boardService.findCard(10L, 1L)).thenReturn(project);
        when(boardService.findColumn(10L, 101L)).thenReturn(roteiro);
        when(kanbanItemService.save(project)).thenReturn(project);

        useCase.execute(10L, 1L, 1L, dto(101L));

        assertSame(roteiro, project.getColumn());
        assertEquals(0, project.getSortOrder());
    }
}
