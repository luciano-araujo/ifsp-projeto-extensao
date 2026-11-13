package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.PublicInviteResponse;
import br.edu.ifsp.tcc.application.entity.TeamInvite;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.InviteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetInviteByTokenUseCaseTest {

    @Mock private InviteService inviteService;
    @InjectMocks private GetInviteByTokenUseCase useCase;

    @Test
    void execute_shouldReturnThePublicData() {
        TeamInvite invite = new TeamInvite();
        PublicInviteResponse mapped = new PublicInviteResponse("Equipe", TeamRole.CREATOR, "a@b.test", "Ana", null);
        when(inviteService.findOpenByToken("tok")).thenReturn(invite);
        when(inviteService.toPublicResponse(invite)).thenReturn(mapped);

        assertSame(mapped, useCase.execute("tok"));
    }

    @Test
    void execute_shouldPropagateNotFoundForExpiredOrUnknown() {
        when(inviteService.findOpenByToken("old")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "x"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute("old"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
