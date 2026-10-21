package br.edu.ifsp.tcc.application.usecase;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoveTeamMemberUseCaseTest {

    @Mock private TeamService teamService;
    @InjectMocks private RemoveTeamMemberUseCase useCase;

    private TeamMember member(Long id, TeamRole role) {
        TeamMember member = new TeamMember();
        member.setId(id);
        member.setRole(role);
        return member;
    }

    @Test
    void execute_shouldForbidCreatorRemovingSomeoneElse() {
        when(teamService.requireMember(10L, 2L)).thenReturn(member(6L, TeamRole.CREATOR));
        when(teamService.findMember(10L, 7L)).thenReturn(member(7L, TeamRole.CREATOR));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 2L, 7L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(teamService, never()).deleteMember(any());
    }

    @Test
    void execute_shouldLetMemberLeaveTheTeam() {
        TeamMember creator = member(6L, TeamRole.CREATOR);
        when(teamService.requireMember(10L, 2L)).thenReturn(creator);
        when(teamService.findMember(10L, 6L)).thenReturn(creator);

        useCase.execute(10L, 2L, 6L);

        verify(teamService).deleteMember(creator);
    }

    @Test
    void execute_shouldNotRemoveTheLastAdmin() {
        TeamMember admin = member(5L, TeamRole.ADMIN);
        when(teamService.requireMember(10L, 1L)).thenReturn(admin);
        when(teamService.findMember(10L, 5L)).thenReturn(admin);
        when(teamService.countAdmins(10L)).thenReturn(1);

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 5L));
        verify(teamService, never()).deleteMember(any());
    }
}
