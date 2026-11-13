package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.RenameTeamDTO;
import br.edu.ifsp.tcc.application.dto.TeamResponse;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RenameTeamUseCaseTest {

    @Mock private TeamService teamService;
    @InjectMocks private RenameTeamUseCase useCase;

    @Test
    void execute_shouldTrimAndReturnTheTeamWithTheCallerRole() {
        Team team = new Team();
        team.setId(10L);
        TeamMember admin = new TeamMember();
        admin.setTeam(team);
        admin.setRole(TeamRole.ADMIN);
        RenameTeamDTO dto = new RenameTeamDTO();
        dto.setName("  Office Resolve Cursos ");
        TeamResponse response = new TeamResponse(10L, "Office Resolve Cursos", TeamRole.ADMIN, 3, null, false);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);
        when(teamService.toTeamResponse(admin)).thenReturn(response);

        assertSame(response, useCase.execute(10L, 1L, dto));
        verify(teamService).renameTeam(team, "Office Resolve Cursos");
    }

    @Test
    void execute_shouldPropagateForbiddenForNonAdmins() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "x"));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 2L, new RenameTeamDTO()));
        verify(teamService, never()).renameTeam(any(), any());
    }
}
