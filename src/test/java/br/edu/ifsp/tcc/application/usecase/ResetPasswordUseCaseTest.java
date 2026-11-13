package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ResetPasswordRequest;
import br.edu.ifsp.tcc.application.entity.PasswordResetToken;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.PasswordResetTokenService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResetPasswordUseCaseTest {

    @Mock private PasswordResetTokenService passwordResetTokenService;
    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private ResetPasswordUseCase useCase;

    private ResetPasswordRequest request(String token) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("ana@example.com");
        request.setToken(token);
        request.setNewPassword("novaSenha1");
        return request;
    }

    private PasswordResetToken token(int attempts, LocalDateTime expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail("ana@example.com");
        token.setToken("123456");
        token.setAttempts(attempts);
        token.setExpiresAt(expiresAt);
        return token;
    }

    @Test
    void execute_shouldChangePasswordAndDiscardCode() {
        User ana = new User();
        ana.setPassword("old-hash");
        when(passwordResetTokenService.findLatestByEmail("ana@example.com"))
                .thenReturn(Optional.of(token(0, LocalDateTime.now().plusMinutes(5))));
        when(userService.findByEmail("ana@example.com")).thenReturn(Optional.of(ana));
        when(passwordEncoder.encode("novaSenha1")).thenReturn("new-hash");

        useCase.execute(request("123456"));

        assertEquals("new-hash", ana.getPassword());
        verify(userService).update(ana);
        verify(passwordResetTokenService).deleteByEmail("ana@example.com");
    }

    @Test
    void execute_shouldCountWrongCodeAndKeepPassword() {
        PasswordResetToken current = token(1, LocalDateTime.now().plusMinutes(5));
        when(passwordResetTokenService.findLatestByEmail("ana@example.com")).thenReturn(Optional.of(current));
        when(passwordResetTokenService.registerWrongAttempt(any(), eq(ResetPasswordUseCase.MAX_ATTEMPTS - 1))).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(request("000000")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(passwordResetTokenService).registerWrongAttempt(any(), eq(ResetPasswordUseCase.MAX_ATTEMPTS - 1));
        verify(userService, never()).update(any());
    }

    @Test
    void execute_shouldDiscardCodeAfterMaxAttempts() {
        PasswordResetToken current = token(ResetPasswordUseCase.MAX_ATTEMPTS - 1, LocalDateTime.now().plusMinutes(5));
        when(passwordResetTokenService.findLatestByEmail("ana@example.com")).thenReturn(Optional.of(current));
        when(passwordResetTokenService.registerWrongAttempt(any(), eq(ResetPasswordUseCase.MAX_ATTEMPTS - 1))).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(request("000000")));

        assertTrue(ex.getReason().contains("Muitas tentativas"));
        verify(passwordResetTokenService).deleteByEmail("ana@example.com");
        verify(userService, never()).update(any());
    }

    @Test
    void execute_shouldRejectExpiredCode() {
        when(passwordResetTokenService.findLatestByEmail("ana@example.com"))
                .thenReturn(Optional.of(token(0, LocalDateTime.now().minusMinutes(1))));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(request("123456")));

        verify(passwordResetTokenService).deleteByEmail("ana@example.com");
        verify(userService, never()).update(any());
    }

    @Test
    void execute_shouldRejectWhenNoCodeWasRequested() {
        when(passwordResetTokenService.findLatestByEmail("ana@example.com")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> useCase.execute(request("123456")));
        verify(userService, never()).update(any());
    }

    @Test
    void execute_shouldRejectNewPasswordOver72Bytes() {
        ResetPasswordRequest request = request("123456");
        request.setNewPassword("é".repeat(37));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("A senha deve ter no máximo 72 bytes (caracteres acentuados contam mais).", ex.getReason());
        verifyNoInteractions(passwordResetTokenService, userService, passwordEncoder);
    }
}
