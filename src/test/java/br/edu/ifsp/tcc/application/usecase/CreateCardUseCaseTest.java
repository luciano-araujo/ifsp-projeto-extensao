package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.dto.CreateCardDTO;
import br.edu.ifsp.tcc.application.entity.BoardColumn;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
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
class CreateCardUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private KanbanItemService kanbanItemService;
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
        when(kanbanItemService.save(any(KanbanItem.class))).thenReturn(saved);
        when(boardService.toCardResponse(saved, 2L, List.of())).thenReturn(response);

        CardResponse result = useCase.execute(10L, user, dto());

        assertSame(response, result);
        verify(kanbanItemService).save(argThatProject(team, column));
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
}
