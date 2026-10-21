package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateCardDTO;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateIdeaUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private CreateIdeaUseCase useCase;

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    @Test
    void execute_shouldSaveIdeaInTeamInbox() {
        Team team = new Team();
        TeamMember creator = new TeamMember();
        creator.setTeam(team);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR)).thenReturn(creator);
        CreateCardDTO dto = new CreateCardDTO();
        dto.setTitle(" Podcast sobre grafos ");
        dto.setTargetAudience("  ");
        dto.setTag("Podcast");

        useCase.execute(10L, user(1L), dto);

        ArgumentCaptor<KanbanItem> saved = ArgumentCaptor.forClass(KanbanItem.class);
        verify(kanbanItemService).save(saved.capture());
        assertEquals("Podcast sobre grafos", saved.getValue().getTitle());
        assertEquals(KanbanItemState.IDEATION, saved.getValue().getState());
        assertSame(team, saved.getValue().getTeam());
        assertNull(saved.getValue().getTargetAudience());
        assertEquals("Podcast", saved.getValue().getTag());
        assertNull(saved.getValue().getColumn());
    }

    @Test
    void execute_shouldBeBlockedForReviewer() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN, TeamRole.CREATOR))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe nao permite esta acao."));
        CreateCardDTO dto = new CreateCardDTO();
        dto.setTitle("Ideia");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, user(2L), dto));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(kanbanItemService, never()).save(any());
    }
}
