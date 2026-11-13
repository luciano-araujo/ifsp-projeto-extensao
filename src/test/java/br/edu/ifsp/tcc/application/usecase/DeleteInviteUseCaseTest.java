package br.edu.ifsp.tcc.application.usecase;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteInviteUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private InviteService inviteService;
    @InjectMocks private DeleteInviteUseCase useCase;

    @Test
    void execute_shouldRemoveTheInviteOfTheTeam() {
        TeamInvite invite = new TeamInvite();
        when(inviteService.findOfTeam(10L, 3L)).thenReturn(invite);

        useCase.execute(10L, 1L, 3L);

        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN);
        verify(inviteService).delete(invite);
    }

    @Test
    void execute_shouldPropagateNotFoundForInviteOfAnotherTeam() {
        when(inviteService.findOfTeam(10L, 99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "x"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(inviteService, never()).delete(any());
    }

    @Test
    void execute_shouldBeForbiddenForNonAdmins() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "x"));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 2L, 3L));
        verifyNoInteractions(inviteService);
    }
}
