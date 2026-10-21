package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ConvertCardDTO;
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
class ConvertIdeaUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private ConvertIdeaUseCase useCase;

    private KanbanItem idea() {
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        item.setTitle("Ideia");
        item.setState(KanbanItemState.IDEATION);
        return item;
    }

    private BoardColumn column(Long id) {
        BoardColumn column = new BoardColumn();
        column.setId(id);
        return column;
    }

    @Test
    void execute_shouldRejectIdeaWithoutAudienceAndObjective() {
        KanbanItem idea = idea();
        when(boardService.findCard(10L, 1L)).thenReturn(idea);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 1L, null, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals(KanbanItemState.IDEATION, idea.getState());
        verify(kanbanItemService, never()).save(any());
    }

    @Test
    void execute_shouldUseBodyFieldsAndPlaceCardInTargetColumn() {
        KanbanItem idea = idea();
        BoardColumn roteiro = column(101L);
        when(boardService.findCard(10L, 1L)).thenReturn(idea);
        when(boardService.resolveColumn(10L, 101L)).thenReturn(roteiro);
        when(kanbanItemService.save(idea)).thenReturn(idea);
        ConvertCardDTO dto = new ConvertCardDTO();
        dto.setTargetAudience(" Iniciantes ");
        dto.setPedagogicalObjective("Entender BFS");

        useCase.execute(10L, 1L, 1L, 101L, dto);

        assertEquals(KanbanItemState.IN_PRODUCTION, idea.getState());
        assertSame(roteiro, idea.getColumn());
        assertEquals("Iniciantes", idea.getTargetAudience());
        verify(boardService).toCardResponse(any(KanbanItem.class), any(), any());
    }

    @Test
    void execute_shouldAcceptFieldsAlreadyFilledOnTheIdea() {
        KanbanItem idea = idea();
        idea.setTargetAudience("Iniciantes");
        idea.setPedagogicalObjective("Entender BFS");
        BoardColumn pesquisa = column(100L);
        when(boardService.findCard(10L, 1L)).thenReturn(idea);
        when(boardService.resolveColumn(10L, null)).thenReturn(pesquisa);
        when(kanbanItemService.save(idea)).thenReturn(idea);

        useCase.execute(10L, 1L, 1L, null, null);

        assertSame(pesquisa, idea.getColumn());
    }

    @Test
    void execute_shouldRejectCardThatIsAlreadyAProject() {
        KanbanItem project = idea();
        project.setState(KanbanItemState.IN_PRODUCTION);
        when(boardService.findCard(10L, 1L)).thenReturn(project);

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 1L, null, null));
        verify(kanbanItemService, never()).save(any());
    }
}
