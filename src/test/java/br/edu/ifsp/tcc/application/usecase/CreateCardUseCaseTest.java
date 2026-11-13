package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.dto.CreateCardDTO;
import br.edu.ifsp.tcc.application.entity.BoardColumn;
import br.edu.ifsp.tcc.application.entity.CardMovement;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.Tag;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TagService;
import br.edu.ifsp.tcc.application.service.PlanService;
import br.edu.ifsp.tcc.application.service.TeamService;
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
class CreateCardUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private KanbanItemService kanbanItemService;
    @Mock private TagService tagService;
    @Mock private PlanService planService;
    @InjectMocks private CreateCardUseCase useCase;

    private User user;

    private CreateCardDTO dto() {
        CreateCardDTO dto = new CreateCardDTO();
        dto.setTitle("  Video de apresentacao  ");
        dto.setTargetAudience(" Alunos ");
        dto.setPedagogicalObjective(" Entender o tema ");
        dto.setColumnId(2L);
        return dto;
    }

    @Test
    void execute_shouldCreateProjectInColumnAndReturnCard() {
        user = new User();
        user.setId(1L);
        Team team = new Team();
        team.setId(10L);
        TeamMember member = new TeamMember();
        member.setTeam(team);
        BoardColumn column = new BoardColumn();
        column.setId(2L);
        KanbanItem saved = new KanbanItem();
        CardResponse response = mock(CardResponse.class);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR)).thenReturn(member);
        when(boardService.resolveColumn(10L, 2L)).thenReturn(column);
        when(boardService.nextSortOrder(10L, 2L)).thenReturn(3);
        Tag tutorial = new Tag();
        CardMovement movement = new CardMovement();
        movement.setEnteredAt(LocalDateTime.of(2026, 11, 1, 14, 10));
        when(kanbanItemService.save(any(KanbanItem.class))).thenReturn(saved);
        when(tagService.resolveTag(10L, 3L, null)).thenReturn(tutorial);
        when(boardService.recordMovement(saved, column, user)).thenReturn(movement);
        when(boardService.toCardResponse(saved, 2L, List.of(), movement.getEnteredAt())).thenReturn(response);
        CreateCardDTO dto = dto();
        dto.setTagId(3L);

        CardResponse result = useCase.execute(10L, user, dto);

        assertSame(response, result);
        verify(kanbanItemService).save(argThatProject(team, column));
        verify(boardService).recordMovement(saved, column, user);
    }

    @Test
    void execute_shouldRejectMissingAudienceOrObjective() {
        user = new User();
        user.setId(1L);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR)).thenReturn(new TeamMember());
        CreateCardDTO dto = dto();
        dto.setTargetAudience("  ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, user, dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(kanbanItemService, never()).save(any());
    }

    private KanbanItem argThatProject(Team team, BoardColumn column) {
        return org.mockito.ArgumentMatchers.argThat(item ->
                item.getState() == KanbanItemState.IN_PRODUCTION
                        && item.getTeam() == team
                        && item.getColumn() == column
                        && "Video de apresentacao".equals(item.getTitle())
                        && "Alunos".equals(item.getTargetAudience())
                        && "Entender o tema".equals(item.getPedagogicalObjective()));
    }

    @Test
    void execute_shouldApplyRn01MinimumsAfterTrim() {
        user = new User();
        user.setId(1L);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR)).thenReturn(new TeamMember());

        CreateCardDTO blankTitle = dto();
        blankTitle.setTitle("   ");
        CreateCardDTO shortAudience = dto();
        shortAudience.setTargetAudience(" ab ");
        CreateCardDTO shortObjective = dto();
        shortObjective.setPedagogicalObjective("  curto   ");

        assertEquals("O título deve ter pelo menos 3 caracteres.", assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, user, blankTitle)).getReason());
        assertEquals("Informe o público-alvo com pelo menos 3 caracteres.", assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, user, shortAudience)).getReason());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, user, shortObjective));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Informe o objetivo pedagógico com pelo menos 10 caracteres.", ex.getReason());
        verify(kanbanItemService, never()).save(any());
    }


    @Test
    void execute_shouldStopWith402WhenTheProjectLimitIsReached() {
        user = new User();
        user.setId(1L);
        Team team = new Team();
        team.setId(10L);
        TeamMember member = new TeamMember();
        member.setTeam(team);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR)).thenReturn(member);
        when(boardService.resolveColumn(10L, 2L)).thenReturn(new BoardColumn());
        doThrow(new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "limite")).when(planService).assertCanCreateActiveProject(team);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, user, dto()));
        assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatusCode());
        verify(kanbanItemService, never()).save(any());
    }
}
