package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.TeamResponse;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.service.InviteService;
import br.edu.ifsp.tcc.application.service.PlanService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptInviteUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private InviteService inviteService;
    @Mock private PlanService planService;
    @InjectMocks private AcceptInviteUseCase useCase;

    private User carla;
    private TeamInvite invite;

    @BeforeEach
    void setUp() {
        Team team = new Team();
        team.setId(10L);
        carla = new User();
        carla.setId(5L);
        carla.setEmail("Carla@Exemplo.test");
        invite = new TeamInvite();
        invite.setTeam(team);
        invite.setEmail("carla@exemplo.test");
        invite.setRole(TeamRole.REVIEWER);
    }

    @Test
    void execute_shouldJoinWithTheRoleOfTheInviteAndMarkItAccepted() {
        TeamResponse response = new TeamResponse(10L, "Equipe", TeamRole.REVIEWER, 2, null, false, null, null, null);
        when(inviteService.findOpenByToken("tok")).thenReturn(invite);
        when(teamService.findMemberByTeamIdAndUserId(10L, 5L)).thenReturn(Optional.empty());
        when(teamService.saveMember(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(teamService.toTeamResponse(any(TeamMember.class))).thenReturn(response);

        TeamResponse result = useCase.execute("tok", carla);

        assertSame(response, result);
        ArgumentCaptor<TeamMember> member = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamService).saveMember(member.capture());
        assertEquals(TeamRole.REVIEWER, member.getValue().getRole());
        assertSame(carla, member.getValue().getUser());
        verify(inviteService).markAccepted(invite);
    }

    @Test
    void execute_shouldForbidAnotherEmail() {
        User other = new User();
        other.setId(6L);
        other.setEmail("outra@exemplo.test");
        when(inviteService.findOpenByToken("tok")).thenReturn(invite);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute("tok", other));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(teamService, never()).saveMember(any());
        verify(inviteService, never()).markAccepted(any());
    }

    @Test
    void execute_shouldConflictWhenAlreadyAMember() {
        when(inviteService.findOpenByToken("tok")).thenReturn(invite);
        when(teamService.findMemberByTeamIdAndUserId(10L, 5L)).thenReturn(Optional.of(new TeamMember()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute("tok", carla));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(inviteService, never()).markAccepted(any());
    }

    @Test
    void execute_shouldPropagateNotFoundForExpiredOrAcceptedInvite() {
        when(inviteService.findOpenByToken("old")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "x"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute("old", carla));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(teamService);
    }

    @Test
    void execute_shouldStopWith402WhenTheTeamIsFull() {
        when(inviteService.findOpenByToken("tok")).thenReturn(invite);
        when(teamService.findMemberByTeamIdAndUserId(10L, 5L)).thenReturn(Optional.empty());
        doThrow(new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "limite")).when(planService).requireCanAddMember(invite.getTeam());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute("tok", carla));
        assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatusCode());
        verify(teamService, never()).saveMember(any());
        verify(inviteService, never()).markAccepted(any());
    }
}
