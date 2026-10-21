package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ForgotPasswordRequest;
import br.edu.ifsp.tcc.application.entity.PasswordResetToken;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.EmailService;
import br.edu.ifsp.tcc.application.service.PasswordResetTokenService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestPasswordResetUseCaseTest {

    @Mock private UserService userService;
    @Mock private PasswordResetTokenService passwordResetTokenService;
    @Mock private EmailService emailService;
    @InjectMocks private RequestPasswordResetUseCase useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "tokenExpiryMinutes", 10);
    }

    private ForgotPasswordRequest request(String email) {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(email);
        return request;
    }

    @Test
    void execute_shouldReplaceOldCodeAndSendNewOneForRegisteredEmail() {
        User ana = new User();
        ana.setName("Ana");
        when(userService.findByEmail("ana@example.com")).thenReturn(Optional.of(ana));

        useCase.execute(request(" ana@example.com "));

        verify(passwordResetTokenService).deleteByEmail("ana@example.com");
        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenService).save(saved.capture());
        assertEquals("ana@example.com", saved.getValue().getEmail());
        assertEquals(6, saved.getValue().getToken().length());
        assertEquals(0, saved.getValue().getAttempts());
        assertNotNull(saved.getValue().getExpiresAt());
        verify(emailService).sendPasswordResetToken(eq("ana@example.com"), eq("Ana"), anyString());
    }

    @Test
    void execute_shouldDoNothingForUnknownEmail() {
        when(userService.findByEmail("ninguem@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> useCase.execute(request("ninguem@example.com")));

        verifyNoInteractions(passwordResetTokenService, emailService);
    }
}
