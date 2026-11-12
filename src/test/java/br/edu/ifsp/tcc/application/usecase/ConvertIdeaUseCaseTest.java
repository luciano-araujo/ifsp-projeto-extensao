package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ConvertCardDTO;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConvertIdeaUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private ConvertIdeaUseCase useCase;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        TeamMember member = new TeamMember();
        member.setUser(user);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR)).thenReturn(member);
    }

    private CardMovement movementAt(LocalDateTime enteredAt) {
        CardMovement movement = new CardMovement();
        movement.setEnteredAt(enteredAt);
        return movement;
    }

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
        LocalDateTime enteredAt = LocalDateTime.of(2026, 11, 1, 14, 10);
        when(boardService.recordMovement(idea, roteiro, user)).thenReturn(movementAt(enteredAt));
        ConvertCardDTO dto = new ConvertCardDTO();
        dto.setTargetAudience(" Iniciantes ");
        dto.setPedagogicalObjective("Entender BFS");

        useCase.execute(10L, 1L, 1L, 101L, dto);

        assertEquals(KanbanItemState.IN_PRODUCTION, idea.getState());
        assertSame(roteiro, idea.getColumn());
        assertEquals("Iniciantes", idea.getTargetAudience());
        verify(boardService).recordMovement(idea, roteiro, user);
        verify(boardService).toCardResponse(idea, 101L, List.of(), enteredAt);
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
        when(boardService.recordMovement(idea, pesquisa, user)).thenReturn(movementAt(LocalDateTime.now()));

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

    @Test
    void execute_shouldRejectTooShortEffectiveAudienceOrObjective() {
        KanbanItem idea = idea();
        idea.setTargetAudience("ab");
        idea.setPedagogicalObjective("Entender BFS");
        when(boardService.findCard(10L, 1L)).thenReturn(idea);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 1L, null, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Informe o público-alvo com pelo menos 3 caracteres.", ex.getReason());

        ConvertCardDTO dto = new ConvertCardDTO();
        dto.setTargetAudience("Iniciantes");
        dto.setPedagogicalObjective("  curto  ");
        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 1L, null, dto));
        assertEquals("Informe o objetivo pedagógico com pelo menos 10 caracteres.", ex2.getReason());
        assertEquals(KanbanItemState.IDEATION, idea.getState());
        verify(kanbanItemService, never()).save(any());
    }

    @Test
    void execute_shouldRejectIdeaWithBlankStoredTitle() {
        KanbanItem idea = idea();
        idea.setTitle("  ");
        idea.setTargetAudience("Iniciantes");
        idea.setPedagogicalObjective("Entender BFS");
        when(boardService.findCard(10L, 1L)).thenReturn(idea);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 1L, null, null));
        assertEquals("O título deve ter pelo menos 3 caracteres.", ex.getReason());
    }
}
