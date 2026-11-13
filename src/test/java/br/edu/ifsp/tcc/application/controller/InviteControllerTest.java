package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.CreateInviteDTO;
import br.edu.ifsp.tcc.application.dto.InviteResponse;
import br.edu.ifsp.tcc.application.dto.TeamResponse;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.AcceptInviteUseCase;
import br.edu.ifsp.tcc.application.usecase.CreateInviteUseCase;
import br.edu.ifsp.tcc.application.usecase.DeleteInviteUseCase;
import br.edu.ifsp.tcc.application.usecase.ListInvitesUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InviteControllerTest {

    @Mock private CreateInviteUseCase createInviteUseCase;
    @Mock private ListInvitesUseCase listInvitesUseCase;
    @Mock private DeleteInviteUseCase deleteInviteUseCase;
    @Mock private AcceptInviteUseCase acceptInviteUseCase;
    @InjectMocks private InviteController controller;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
    }

    @Test
    void create_shouldReturnInvite() {
        CreateInviteDTO dto = new CreateInviteDTO();
        InviteResponse invite = new InviteResponse(3L, "a@b.test", null, null, null, null, null);
        when(createInviteUseCase.execute(10L, 1L, dto)).thenReturn(invite);

        ResponseEntity<InviteResponse> response = controller.create(user, 10L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(invite, response.getBody());
    }

    @Test
    void list_shouldReturnInvites() {
        List<InviteResponse> invites = List.of();
        when(listInvitesUseCase.execute(10L, 1L)).thenReturn(invites);

        assertSame(invites, controller.list(user, 10L).getBody());
    }

    @Test
    void delete_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.delete(user, 10L, 3L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteInviteUseCase).execute(10L, 1L, 3L);
    }

    @Test
    void accept_shouldReturnTeam() {
        TeamResponse team = new TeamResponse(10L, "Equipe", null, 2, null, false, null, null, null);
        when(acceptInviteUseCase.execute("tok", user)).thenReturn(team);

        ResponseEntity<TeamResponse> response = controller.accept(user, "tok");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(team, response.getBody());
    }
}
