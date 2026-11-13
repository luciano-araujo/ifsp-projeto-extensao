package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.AddTeamMemberDTO;
import br.edu.ifsp.tcc.application.dto.CreateInviteDTO;
import br.edu.ifsp.tcc.application.dto.InviteResponse;
import br.edu.ifsp.tcc.application.entity.InviteStatus;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.InviteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddTeamMemberUseCaseTest {

    @Mock private InviteService inviteService;
    @InjectMocks private AddTeamMemberUseCase useCase;

    private AddTeamMemberDTO dto(String email, TeamRole role) {
        AddTeamMemberDTO dto = new AddTeamMemberDTO();
        dto.setEmail(email);
        dto.setRole(role);
        return dto;
    }

    @Test
    void execute_shouldCreateAnInviteWithTheSameEmailAndRoleInsteadOfAMember() {
        InviteResponse response = new InviteResponse(3L, "rui@test.com", TeamRole.REVIEWER,
                InviteStatus.PENDING, null, null, null);
        when(inviteService.createInvite(eq(10L), eq(1L), any(CreateInviteDTO.class))).thenReturn(response);

        InviteResponse result = useCase.execute(10L, 1L, dto("rui@test.com", TeamRole.REVIEWER));

        assertSame(response, result);
        ArgumentCaptor<CreateInviteDTO> sent = ArgumentCaptor.forClass(CreateInviteDTO.class);
        verify(inviteService).createInvite(eq(10L), eq(1L), sent.capture());
        assertEquals("rui@test.com", sent.getValue().getEmail());
        assertEquals(TeamRole.REVIEWER, sent.getValue().getRole());
    }

    @Test
    void execute_shouldPropagateTheInviteRulesAsTheyAre() {
        when(inviteService.createInvite(eq(10L), eq(1L), any(CreateInviteDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "limite"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, dto("rui@test.com", TeamRole.CREATOR)));
        assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatusCode());
    }
}
