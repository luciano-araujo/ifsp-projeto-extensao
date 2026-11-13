package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.InviteResponse;
import br.edu.ifsp.tcc.application.dto.PublicInviteResponse;
import br.edu.ifsp.tcc.application.entity.TeamInvite;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.InviteService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** List, delete and public lookup of invites: thin use cases, tested together. */
@ExtendWith(MockitoExtension.class)
class InviteRoutesUseCasesTest {

    @Mock private TeamService teamService;
    @Mock private InviteService inviteService;

    @Test
    void list_shouldReturnTheOpenInvitesOfTheTeamForAdmins() {
        TeamInvite invite = new TeamInvite();
        InviteResponse mapped = new InviteResponse(1L, "a@b.test", TeamRole.CREATOR, null, null, null, null);
        when(inviteService.findOpenByTeamId(10L)).thenReturn(List.of(invite));
        when(inviteService.toInviteResponse(invite)).thenReturn(mapped);

        List<InviteResponse> result = new ListInvitesUseCase(teamService, inviteService).execute(10L, 1L);

        assertEquals(List.of(mapped), result);
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN);
    }

    @Test
    void list_shouldBeForbiddenForNonAdmins() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "x"));
        ListInvitesUseCase useCase = new ListInvitesUseCase(teamService, inviteService);

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 2L));
        verifyNoInteractions(inviteService);
    }

    @Test
    void delete_shouldRemoveTheInviteOfTheTeam() {
        TeamInvite invite = new TeamInvite();
        when(inviteService.findOfTeam(10L, 3L)).thenReturn(invite);

        new DeleteInviteUseCase(teamService, inviteService).execute(10L, 1L, 3L);

        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN);
        verify(inviteService).delete(invite);
    }

    @Test
    void delete_shouldPropagateNotFoundForInviteOfAnotherTeam() {
        when(inviteService.findOfTeam(10L, 99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "x"));
        DeleteInviteUseCase useCase = new DeleteInviteUseCase(teamService, inviteService);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(inviteService, never()).delete(any());
    }

    @Test
    void getByToken_shouldReturnThePublicData() {
        TeamInvite invite = new TeamInvite();
        PublicInviteResponse mapped = new PublicInviteResponse("Equipe", TeamRole.CREATOR, "a@b.test", "Ana", null);
        when(inviteService.findOpenByToken("tok")).thenReturn(invite);
        when(inviteService.toPublicResponse(invite)).thenReturn(mapped);

        assertSame(mapped, new GetInviteByTokenUseCase(inviteService).execute("tok"));
    }

    @Test
    void getByToken_shouldPropagateNotFoundForExpiredOrUnknown() {
        when(inviteService.findOpenByToken("old")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "x"));
        GetInviteByTokenUseCase useCase = new GetInviteByTokenUseCase(inviteService);

        assertThrows(ResponseStatusException.class, () -> useCase.execute("old"));
    }
}
