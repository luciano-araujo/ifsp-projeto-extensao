package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
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
class DeleteTeamUseCaseTest {

    @Mock private TeamService teamService;
    @InjectMocks private DeleteTeamUseCase useCase;

    private Team team;
    private TeamMember admin;

    @BeforeEach
    void setUp() {
        team = new Team();
        team.setId(10L);
        admin = new TeamMember();
        admin.setTeam(team);
        admin.setRole(TeamRole.ADMIN);
    }

    @Test
    void execute_shouldDeleteThroughTeamService() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);

        useCase.execute(10L, 1L);

        verify(teamService).requireDeletable(team);
        verify(teamService).deleteTeam(team);
    }

    @Test
    void execute_shouldRefuseThePersonalTeam() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "A equipe pessoal não pode ser excluída."))
                .when(teamService).requireDeletable(team);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(teamService, never()).deleteTeam(any());
    }

    @Test
    void execute_shouldPropagateForbiddenForNonAdmins() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "x"));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 2L));
        verify(teamService, never()).deleteTeam(any());
    }
}
