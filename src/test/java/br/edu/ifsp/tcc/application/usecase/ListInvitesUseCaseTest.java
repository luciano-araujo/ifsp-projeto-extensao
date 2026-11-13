package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.InviteResponse;
import br.edu.ifsp.tcc.application.entity.TeamInvite;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.InviteService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListInvitesUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private InviteService inviteService;
    @InjectMocks private ListInvitesUseCase useCase;

    @Test
    void execute_shouldReturnTheOpenInvitesOfTheTeamForAdmins() {
        TeamInvite invite = new TeamInvite();
        InviteResponse mapped = new InviteResponse(1L, "a@b.test", TeamRole.CREATOR, null, null, null, null);
        when(inviteService.findOpenByTeamId(10L)).thenReturn(List.of(invite));
        when(inviteService.toInviteResponse(invite)).thenReturn(mapped);

        assertEquals(List.of(mapped), useCase.execute(10L, 1L));
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN);
    }

    @Test
    void execute_shouldBeForbiddenForNonAdmins() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "x"));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 2L));
        verifyNoInteractions(inviteService);
    }
}
