package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ConfirmEmailChangeDTO;
import br.edu.ifsp.tcc.application.entity.EmailChangeToken;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.EmailChangeTokenService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmEmailChangeUseCaseTest {

    @Mock private UserService userService;
    @Mock private EmailChangeTokenService emailChangeTokenService;
    @InjectMocks private ConfirmEmailChangeUseCase useCase;

    private User ana;

    @BeforeEach
    void setUp() {
        ana = new User();
        ana.setId(1L);
        ana.setEmail("ana@exemplo.test");
    }

    private ConfirmEmailChangeDTO dto(String code) {
        ConfirmEmailChangeDTO dto = new ConfirmEmailChangeDTO();
        dto.setToken(code);
        return dto;
    }

    private EmailChangeToken pending(int attempts, LocalDateTime expiresAt) {
        EmailChangeToken token = new EmailChangeToken();
        token.setUser(ana);
        token.setNewEmail("novo@exemplo.test");
        token.setToken("482917");
        token.setAttempts(attempts);
        token.setExpiresAt(expiresAt);
        return token;
    }

    private ResponseStatusException thrown(Long id, Long caller, String code) {
        return assertThrows(ResponseStatusException.class, () -> useCase.execute(id, caller, dto(code)));
    }

    @Test
    void execute_shouldSwitchTheEmailAndDiscardTheCode() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(emailChangeTokenService.findByUserId(1L)).thenReturn(Optional.of(pending(0, LocalDateTime.now().plusMinutes(5))));
        when(userService.findByEmail("novo@exemplo.test")).thenReturn(Optional.empty());
        when(userService.update(ana)).thenReturn(ana);

        User result = useCase.execute(1L, 1L, dto("482917"));

        assertSame(ana, result);
        assertEquals("novo@exemplo.test", ana.getEmail());
        verify(emailChangeTokenService).deleteByUserId(1L);
    }

    @Test
    void execute_shouldCountAWrongCode() {
        EmailChangeToken token = pending(1, LocalDateTime.now().plusMinutes(5));
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(emailChangeTokenService.findByUserId(1L)).thenReturn(Optional.of(token));
        when(emailChangeTokenService.registerWrongAttempt(any(), eq(ConfirmEmailChangeUseCase.MAX_ATTEMPTS - 1))).thenReturn(true);

        ResponseStatusException ex = thrown(1L, 1L, "000000");

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Código inválido.", ex.getReason());
        verify(emailChangeTokenService).registerWrongAttempt(any(), eq(ConfirmEmailChangeUseCase.MAX_ATTEMPTS - 1));
        assertEquals("ana@exemplo.test", ana.getEmail());
    }

    @Test
    void execute_shouldDiscardTheCodeOnTheFifthWrongAttempt() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(emailChangeTokenService.findByUserId(1L)).thenReturn(Optional.of(
                pending(ConfirmEmailChangeUseCase.MAX_ATTEMPTS - 1, LocalDateTime.now().plusMinutes(5))));
        when(emailChangeTokenService.registerWrongAttempt(any(), eq(ConfirmEmailChangeUseCase.MAX_ATTEMPTS - 1))).thenReturn(false);

        ResponseStatusException ex = thrown(1L, 1L, "000000");

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().startsWith("Muitas tentativas"));
        verify(emailChangeTokenService).deleteByUserId(1L);
        verify(emailChangeTokenService, never()).save(any());
    }

    @Test
    void execute_shouldRejectAnExpiredCode() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(emailChangeTokenService.findByUserId(1L)).thenReturn(Optional.of(pending(0, LocalDateTime.now().minusMinutes(1))));

        assertEquals(HttpStatus.BAD_REQUEST, thrown(1L, 1L, "482917").getStatusCode());
        verify(emailChangeTokenService).deleteByUserId(1L);
    }

    @Test
    void execute_shouldRejectWhenThereIsNoPendingRequest() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(emailChangeTokenService.findByUserId(1L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.BAD_REQUEST, thrown(1L, 1L, "482917").getStatusCode());
    }

    @Test
    void execute_shouldConflictWhenSomeoneTookTheAddressMeanwhile() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(emailChangeTokenService.findByUserId(1L)).thenReturn(Optional.of(pending(0, LocalDateTime.now().plusMinutes(5))));
        when(userService.findByEmail("novo@exemplo.test")).thenReturn(Optional.of(new User()));

        assertEquals(HttpStatus.CONFLICT, thrown(1L, 1L, "482917").getStatusCode());
        assertEquals("ana@exemplo.test", ana.getEmail());
        verify(emailChangeTokenService).deleteByUserId(1L);
    }

    @Test
    void execute_shouldForbidOtherUsers() {
        assertEquals(HttpStatus.FORBIDDEN, thrown(1L, 2L, "482917").getStatusCode());
        verifyNoInteractions(userService);
    }

    @Test
    void execute_shouldReturn404ForUnknownUser() {
        when(userService.findById(1L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, thrown(1L, 1L, "482917").getStatusCode());
    }
}
