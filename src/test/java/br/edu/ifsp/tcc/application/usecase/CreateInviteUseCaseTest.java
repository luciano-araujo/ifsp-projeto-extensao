package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateInviteDTO;
import br.edu.ifsp.tcc.application.dto.InviteResponse;
import br.edu.ifsp.tcc.application.entity.InviteStatus;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateInviteUseCaseTest {

    @Mock private InviteService inviteService;
    @InjectMocks private CreateInviteUseCase useCase;

    private CreateInviteDTO dto() {
        CreateInviteDTO dto = new CreateInviteDTO();
        dto.setEmail("carla@exemplo.test");
        dto.setRole(TeamRole.REVIEWER);
        return dto;
    }

    @Test
    void execute_shouldDelegateToTheSharedInviteLogic() {
        CreateInviteDTO dto = dto();
        InviteResponse response = new InviteResponse(3L, "carla@exemplo.test", TeamRole.REVIEWER,
                InviteStatus.PENDING, null, null, null);
        when(inviteService.createInvite(10L, 1L, dto)).thenReturn(response);

        assertSame(response, useCase.execute(10L, 1L, dto));
    }

    @Test
    void execute_shouldPropagateTheServiceErrors() {
        CreateInviteDTO dto = dto();
        when(inviteService.createInvite(10L, 2L, dto))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe não permite esta ação."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 2L, dto));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}
