package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ChangePasswordDTO;
import br.edu.ifsp.tcc.application.dto.ConfirmEmailChangeDTO;
import br.edu.ifsp.tcc.application.dto.MessageResponse;
import br.edu.ifsp.tcc.application.dto.RequestEmailChangeDTO;
import br.edu.ifsp.tcc.application.entity.EmailChangeToken;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.EmailChangeTokenService;
import br.edu.ifsp.tcc.application.service.EmailService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Password change and the two steps of the e-mail change (7.13). */
@ExtendWith(MockitoExtension.class)
class AccountUseCasesTest {

    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailChangeTokenService emailChangeTokenService;
    @Mock private EmailService emailService;

    private User ana;

    @BeforeEach
    void setUp() {
        ana = new User();
        ana.setId(1L);
        ana.setName("Ana");
        ana.setEmail("ana@exemplo.test");
        ana.setPassword("old-hash");
    }

    private ResponseStatusException status(HttpStatus status, Throwable t) {
        ResponseStatusException ex = assertInstanceOf(ResponseStatusException.class, t);
        assertEquals(status, ex.getStatusCode());
        return ex;
    }

    // --- Change password ---

    private ChangePasswordDTO passwordDto(String current, String next) {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setCurrentPassword(current);
        dto.setNewPassword(next);
        return dto;
    }

    @Test
    void changePassword_shouldEncodeAndSaveTheNewPassword() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(passwordEncoder.matches("atual123", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("nova1234")).thenReturn("new-hash");

        new ChangePasswordUseCase(userService, passwordEncoder).execute(1L, 1L, passwordDto("atual123", "nova1234"));

        assertEquals("new-hash", ana.getPassword());
        verify(userService).update(ana);
    }

    @Test
    void changePassword_shouldForbidOtherUsers() {
        ChangePasswordUseCase useCase = new ChangePasswordUseCase(userService, passwordEncoder);

        status(HttpStatus.FORBIDDEN, assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 2L, passwordDto("a", "bbbbbb"))));
        verifyNoInteractions(userService);
    }

    @Test
    void changePassword_shouldReturn404ForUnknownUser() {
        when(userService.findById(1L)).thenReturn(Optional.empty());
        ChangePasswordUseCase useCase = new ChangePasswordUseCase(userService, passwordEncoder);

        status(HttpStatus.NOT_FOUND, assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 1L, passwordDto("a", "bbbbbb"))));
    }

    @Test
    void changePassword_shouldReturn400WhenCurrentPasswordIsWrong() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(passwordEncoder.matches("errada", "old-hash")).thenReturn(false);
        ChangePasswordUseCase useCase = new ChangePasswordUseCase(userService, passwordEncoder);

        ResponseStatusException ex = status(HttpStatus.BAD_REQUEST, assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 1L, passwordDto("errada", "nova1234"))));

        assertEquals("Senha atual incorreta.", ex.getReason());
        verify(userService, never()).update(any());
    }

    @Test
    void changePassword_shouldTreatACurrentPasswordOver72BytesAsWrong() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        ChangePasswordUseCase useCase = new ChangePasswordUseCase(userService, passwordEncoder);

        status(HttpStatus.BAD_REQUEST, assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 1L, passwordDto("a".repeat(73), "nova1234"))));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void changePassword_shouldRejectANewPasswordOver72Bytes() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(passwordEncoder.matches("atual123", "old-hash")).thenReturn(true);
        ChangePasswordUseCase useCase = new ChangePasswordUseCase(userService, passwordEncoder);

        // 37 accented characters = 74 bytes
        status(HttpStatus.BAD_REQUEST, assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 1L, passwordDto("atual123", "é".repeat(37)))));
        verify(userService, never()).update(any());
    }

    // --- Request e-mail change ---

    private RequestEmailChangeUseCase requestUseCase() {
        RequestEmailChangeUseCase useCase = new RequestEmailChangeUseCase(userService, emailChangeTokenService, emailService);
        ReflectionTestUtils.setField(useCase, "tokenExpiryMinutes", 10);
        return useCase;
    }

    private RequestEmailChangeDTO requestDto(String email) {
        RequestEmailChangeDTO dto = new RequestEmailChangeDTO();
        dto.setNewEmail(email);
        return dto;
    }

    @Test
    void requestEmailChange_shouldStoreCodeAndSendItToTheNewAddress() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(userService.findByEmail("novo@exemplo.test")).thenReturn(Optional.empty());

        MessageResponse response = requestUseCase().execute(1L, 1L, requestDto(" novo@exemplo.test "));

        assertEquals("Enviamos um código de verificação para novo@exemplo.test.", response.getMessage());
        verify(emailChangeTokenService).deleteByUserId(1L);
        ArgumentCaptor<EmailChangeToken> saved = ArgumentCaptor.forClass(EmailChangeToken.class);
        verify(emailChangeTokenService).save(saved.capture());
        assertEquals("novo@exemplo.test", saved.getValue().getNewEmail());
        assertTrue(saved.getValue().getToken().matches("\\d{6}"));
        assertTrue(saved.getValue().getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(9)));
        verify(emailService).sendEmailChangeToken("novo@exemplo.test", "Ana", saved.getValue().getToken());
    }

    @Test
    void requestEmailChange_shouldConflictWhenEmailBelongsToSomeone() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(userService.findByEmail("bia@exemplo.test")).thenReturn(Optional.of(new User()));
        RequestEmailChangeUseCase useCase = requestUseCase();

        status(HttpStatus.CONFLICT, assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 1L, requestDto("bia@exemplo.test"))));
        verifyNoInteractions(emailService, emailChangeTokenService);
    }

    @Test
    void requestEmailChange_shouldConflictWhenEmailIsTheCurrentOne() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        RequestEmailChangeUseCase useCase = requestUseCase();

        status(HttpStatus.CONFLICT, assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 1L, requestDto("ANA@exemplo.test"))));
    }

    @Test
    void requestEmailChange_shouldForbidOtherUsersAnd404UnknownUser() {
        RequestEmailChangeUseCase useCase = requestUseCase();
        status(HttpStatus.FORBIDDEN, assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 2L, requestDto("x@exemplo.test"))));

        when(userService.findById(1L)).thenReturn(Optional.empty());
        status(HttpStatus.NOT_FOUND, assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 1L, requestDto("x@exemplo.test"))));
    }

    // --- Confirm e-mail change ---

    private ConfirmEmailChangeUseCase confirmUseCase() {
        return new ConfirmEmailChangeUseCase(userService, emailChangeTokenService);
    }

    private ConfirmEmailChangeDTO confirmDto(String code) {
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

    @Test
    void confirmEmailChange_shouldSwitchTheEmailAndDiscardTheCode() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(emailChangeTokenService.findByUserId(1L)).thenReturn(Optional.of(pending(0, LocalDateTime.now().plusMinutes(5))));
        when(userService.findByEmail("novo@exemplo.test")).thenReturn(Optional.empty());
        when(userService.update(ana)).thenReturn(ana);

        User result = confirmUseCase().execute(1L, 1L, confirmDto("482917"));

        assertSame(ana, result);
        assertEquals("novo@exemplo.test", ana.getEmail());
        verify(emailChangeTokenService).deleteByUserId(1L);
    }

    @Test
    void confirmEmailChange_shouldCountAWrongCode() {
        EmailChangeToken token = pending(1, LocalDateTime.now().plusMinutes(5));
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(emailChangeTokenService.findByUserId(1L)).thenReturn(Optional.of(token));
        ConfirmEmailChangeUseCase useCase = confirmUseCase();

        ResponseStatusException ex = status(HttpStatus.BAD_REQUEST, assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 1L, confirmDto("000000"))));

        assertEquals("Código inválido.", ex.getReason());
        assertEquals(2, token.getAttempts());
        verify(emailChangeTokenService).save(token);
        assertEquals("ana@exemplo.test", ana.getEmail());
    }

    @Test
    void confirmEmailChange_shouldDiscardTheCodeOnTheFifthWrongAttempt() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(emailChangeTokenService.findByUserId(1L))
                .thenReturn(Optional.of(pending(ConfirmEmailChangeUseCase.MAX_ATTEMPTS - 1, LocalDateTime.now().plusMinutes(5))));
        ConfirmEmailChangeUseCase useCase = confirmUseCase();

        ResponseStatusException ex = status(HttpStatus.BAD_REQUEST, assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 1L, confirmDto("000000"))));

        assertTrue(ex.getReason().startsWith("Muitas tentativas"));
        verify(emailChangeTokenService).deleteByUserId(1L);
        verify(emailChangeTokenService, never()).save(any());
    }

    @Test
    void confirmEmailChange_shouldRejectAnExpiredCode() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(emailChangeTokenService.findByUserId(1L)).thenReturn(Optional.of(pending(0, LocalDateTime.now().minusMinutes(1))));
        ConfirmEmailChangeUseCase useCase = confirmUseCase();

        status(HttpStatus.BAD_REQUEST, assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 1L, confirmDto("482917"))));
        verify(emailChangeTokenService).deleteByUserId(1L);
    }

    @Test
    void confirmEmailChange_shouldRejectWhenThereIsNoPendingRequest() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(emailChangeTokenService.findByUserId(1L)).thenReturn(Optional.empty());
        ConfirmEmailChangeUseCase useCase = confirmUseCase();

        status(HttpStatus.BAD_REQUEST, assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 1L, confirmDto("482917"))));
    }

    @Test
    void confirmEmailChange_shouldConflictWhenSomeoneTookTheAddressMeanwhile() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(emailChangeTokenService.findByUserId(1L)).thenReturn(Optional.of(pending(0, LocalDateTime.now().plusMinutes(5))));
        when(userService.findByEmail("novo@exemplo.test")).thenReturn(Optional.of(new User()));
        ConfirmEmailChangeUseCase useCase = confirmUseCase();

        status(HttpStatus.CONFLICT, assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 1L, confirmDto("482917"))));
        assertEquals("ana@exemplo.test", ana.getEmail());
        verify(emailChangeTokenService).deleteByUserId(1L);
    }

    @Test
    void confirmEmailChange_shouldForbidOtherUsersAnd404UnknownUser() {
        ConfirmEmailChangeUseCase useCase = confirmUseCase();
        status(HttpStatus.FORBIDDEN, assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 2L, confirmDto("482917"))));

        when(userService.findById(1L)).thenReturn(Optional.empty());
        status(HttpStatus.NOT_FOUND, assertThrows(RuntimeException.class,
                () -> useCase.execute(1L, 1L, confirmDto("482917"))));
    }
}
