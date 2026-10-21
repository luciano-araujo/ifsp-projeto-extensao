package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.AddTeamMemberDTO;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.TeamService;
import br.edu.ifsp.tcc.application.service.UserService;
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
class AddTeamMemberUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private UserService userService;
    @InjectMocks private AddTeamMemberUseCase useCase;

    private TeamMember admin() {
        TeamMember admin = new TeamMember();
        admin.setTeam(new Team());
        admin.setRole(TeamRole.ADMIN);
        return admin;
    }

    private AddTeamMemberDTO dto(String email, TeamRole role) {
        AddTeamMemberDTO dto = new AddTeamMemberDTO();
        dto.setEmail(email);
        dto.setRole(role);
        return dto;
    }

    @Test
    void execute_shouldReturnNotFoundWhenEmailIsNotRegistered() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin());
        when(userService.findByEmail("ninguem@test.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, dto("ninguem@test.com", TeamRole.CREATOR)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(teamService, never()).saveMember(any());
    }

    @Test
    void execute_shouldRejectUserAlreadyInTeam() {
        User rui = new User();
        rui.setId(2L);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin());
        when(userService.findByEmail("rui@test.com")).thenReturn(Optional.of(rui));
        when(teamService.findMemberByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(new TeamMember()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, dto("rui@test.com", TeamRole.REVIEWER)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void execute_shouldSaveMemberWithRequestedRole() {
        User rui = new User();
        rui.setId(2L);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin());
        when(userService.findByEmail("rui@test.com")).thenReturn(Optional.of(rui));
        when(teamService.findMemberByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        useCase.execute(10L, 1L, dto(" rui@test.com ", TeamRole.REVIEWER));

        ArgumentCaptor<TeamMember> saved = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamService).saveMember(saved.capture());
        assertEquals(TeamRole.REVIEWER, saved.getValue().getRole());
        assertSame(rui, saved.getValue().getUser());
    }

    @Test
    void execute_shouldBeBlockedForNonAdmin() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe nao permite esta acao."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 2L, dto("x@test.com", TeamRole.CREATOR)));
        verifyNoInteractions(userService);
    }
}
