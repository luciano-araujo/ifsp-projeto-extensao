package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.UpdateTeamMemberDTO;
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
class UpdateTeamMemberRoleUseCaseTest {

    @Mock private TeamService teamService;
    @InjectMocks private UpdateTeamMemberRoleUseCase useCase;

    private UpdateTeamMemberDTO dto(TeamRole role) {
        UpdateTeamMemberDTO dto = new UpdateTeamMemberDTO();
        dto.setRole(role);
        return dto;
    }

    @Test
    void execute_shouldKeepAtLeastOneAdmin() {
        TeamMember admin = new TeamMember();
        admin.setRole(TeamRole.ADMIN);
        when(teamService.findMember(10L, 5L)).thenReturn(admin);
        when(teamService.countAdmins(10L)).thenReturn(1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 5L, dto(TeamRole.CREATOR)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals(TeamRole.ADMIN, admin.getRole());
        verify(teamService, never()).saveMember(any());
    }

    @Test
    void execute_shouldChangeRole() {
        TeamMember creator = new TeamMember();
        creator.setRole(TeamRole.CREATOR);
        when(teamService.findMember(10L, 6L)).thenReturn(creator);

        useCase.execute(10L, 1L, 6L, dto(TeamRole.REVIEWER));

        assertEquals(TeamRole.REVIEWER, creator.getRole());
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN);
        verify(teamService).saveMember(creator);
    }
}
