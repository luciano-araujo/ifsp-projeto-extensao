package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.RenameTeamDTO;
import br.edu.ifsp.tcc.application.dto.TeamResponse;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RenameAndDeleteTeamUseCasesTest {

    @Mock private TeamService teamService;

    private Team team;
    private TeamMember admin;

    @BeforeEach
    void setUp() {
        team = new Team();
        team.setId(10L);
        team.setName("Equipe");
        admin = new TeamMember();
        admin.setTeam(team);
        admin.setRole(TeamRole.ADMIN);
    }

    @Test
    void rename_shouldTrimAndReturnTheTeamWithTheCallerRole() {
        RenameTeamDTO dto = new RenameTeamDTO();
        dto.setName("  Office Resolve Cursos ");
        TeamResponse response = new TeamResponse(10L, "Office Resolve Cursos", TeamRole.ADMIN, 3, null, false);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);
        when(teamService.toTeamResponse(admin)).thenReturn(response);

        TeamResponse result = new RenameTeamUseCase(teamService).execute(10L, 1L, dto);

        assertSame(response, result);
        verify(teamService).renameTeam(team, "Office Resolve Cursos");
    }

    @Test
    void rename_shouldPropagateForbiddenForNonAdmins() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "x"));
        RenameTeamUseCase useCase = new RenameTeamUseCase(teamService);

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 2L, new RenameTeamDTO()));
        verify(teamService, never()).renameTeam(any(), any());
    }

    @Test
    void delete_shouldDeleteThroughTeamService() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);

        new DeleteTeamUseCase(teamService).execute(10L, 1L);

        verify(teamService).requireDeletable(team);
        verify(teamService).deleteTeam(team);
    }

    @Test
    void delete_shouldRefuseThePersonalTeam() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "A equipe pessoal não pode ser excluída."))
                .when(teamService).requireDeletable(team);
        DeleteTeamUseCase useCase = new DeleteTeamUseCase(teamService);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(teamService, never()).deleteTeam(any());
    }

    @Test
    void delete_shouldPropagateForbiddenForNonAdmins() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "x"));
        DeleteTeamUseCase useCase = new DeleteTeamUseCase(teamService);

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 2L));
        verify(teamService, never()).deleteTeam(any());
    }
}
