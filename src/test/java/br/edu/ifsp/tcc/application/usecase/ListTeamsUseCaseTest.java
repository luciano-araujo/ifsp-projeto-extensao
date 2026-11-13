package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.TeamResponse;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListTeamsUseCaseTest {

    @Mock private TeamService teamService;
    @InjectMocks private ListTeamsUseCase useCase;

    private User ana() {
        User user = new User();
        user.setId(1L);
        user.setName("Ana Souza");
        return user;
    }

    private TeamMember membership(Team team) {
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setRole(TeamRole.ADMIN);
        return member;
    }

    @Test
    void execute_shouldCreatePersonalTeamWhenUserHasNone() {
        User ana = ana();
        Team team = new Team();
        team.setId(10L);
        when(teamService.findMembershipsByUserId(1L))
                .thenReturn(List.of())
                .thenReturn(List.of(membership(team)));

        useCase.execute(ana);

        verify(teamService).createPersonalTeam(ana);
        verify(teamService).toTeamResponse(any(TeamMember.class));
    }

    @Test
    void execute_shouldListExistingTeamsWithoutCreatingAnother() {
        Team team = new Team();
        team.setId(10L);
        TeamMember member = membership(team);
        TeamResponse response = new TeamResponse(10L, "Estúdio de Ana", TeamRole.ADMIN, 1, null, false);
        when(teamService.findMembershipsByUserId(1L)).thenReturn(List.of(member));
        when(teamService.toTeamResponse(member)).thenReturn(response);

        List<TeamResponse> result = useCase.execute(ana());

        assertEquals(List.of(response), result);
        verify(teamService, never()).createPersonalTeam(any());
    }
}
