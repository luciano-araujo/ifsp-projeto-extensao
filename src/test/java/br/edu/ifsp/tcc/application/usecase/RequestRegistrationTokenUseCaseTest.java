package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.RegisterTokenRequest;
import br.edu.ifsp.tcc.application.entity.RegistrationToken;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.EmailService;
import br.edu.ifsp.tcc.application.service.RegistrationTokenService;
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
class RequestRegistrationTokenUseCaseTest {

    @Mock private UserService userService;
    @Mock private RegistrationTokenService registrationTokenService;
    @Mock private EmailService emailService;
    @InjectMocks private RequestRegistrationTokenUseCase useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "tokenExpiryMinutes", 10);
    }

    private RegisterTokenRequest request(String name, String email) {
        RegisterTokenRequest request = new RegisterTokenRequest();
        request.setName(name);
        request.setEmail(email);
        return request;
    }

    @Test
    void execute_shouldCreateTokenAndSendEmail() {
        when(userService.findByEmail("new@example.com")).thenReturn(Optional.empty());

        useCase.execute(request("New User", "new@example.com"));

        verify(registrationTokenService).deleteByEmail("new@example.com");

        ArgumentCaptor<RegistrationToken> tokenCaptor = ArgumentCaptor.forClass(RegistrationToken.class);
        verify(registrationTokenService).save(tokenCaptor.capture());

        RegistrationToken saved = tokenCaptor.getValue();
        assertEquals("new@example.com", saved.getEmail());
        assertEquals("New User", saved.getName());
        assertEquals(6, saved.getToken().length());
        assertNotNull(saved.getExpiresAt());

        verify(emailService).sendRegistrationToken(eq("new@example.com"), eq("New User"), anyString());
    }

    @Test
    void execute_shouldThrowWhenEmailAlreadyRegistered() {
        when(userService.findByEmail("existing@example.com")).thenReturn(Optional.of(new User()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> useCase.execute(request("User", "existing@example.com")));
        assertTrue(ex.getMessage().contains("ja esta registrado"));

        verify(registrationTokenService, never()).save(any());
        verify(emailService, never()).sendRegistrationToken(any(), any(), any());
    }
}
