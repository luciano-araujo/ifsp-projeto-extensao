package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CreateUserDTO;
import br.edu.ifsp.tcc.application.entity.RegistrationToken;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.repository.RegistrationTokenRepository;
import br.edu.ifsp.tcc.application.repository.UserRepository;
import br.edu.ifsp.tcc.application.usecase.CreateUserUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RegistrationTokenRepository registrationTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private CreateUserUseCase createUserUseCase;

    @InjectMocks private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "tokenExpiryMinutes", 10);
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setName("Test");
        user.setPassword("encoded-password");
        return user;
    }

    // authenticate tests

    @Test
    void authenticate_shouldReturnUserWhenCredentialsAreValid() {
        User user = createUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-password", "encoded-password")).thenReturn(true);

        User result = authService.authenticate("test@example.com", "raw-password");

        assertEquals(user, result);
    }

    @Test
    void authenticate_shouldThrowWhenEmailNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.authenticate("unknown@example.com", "password"));
        assertTrue(ex.getMessage().contains("Credenciais invalidas"));
    }

    @Test
    void authenticate_shouldThrowWhenPasswordIsWrong() {
        User user = createUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.authenticate("test@example.com", "wrong-password"));
        assertTrue(ex.getMessage().contains("Credenciais invalidas"));
    }

    // generateRegistrationToken tests

    @Test
    void generateRegistrationToken_shouldCreateTokenAndSendEmail() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        authService.generateRegistrationToken("New User", "new@example.com");

        verify(registrationTokenRepository).deleteByEmail("new@example.com");

        ArgumentCaptor<RegistrationToken> tokenCaptor = ArgumentCaptor.forClass(RegistrationToken.class);
        verify(registrationTokenRepository).save(tokenCaptor.capture());

        RegistrationToken saved = tokenCaptor.getValue();
        assertEquals("new@example.com", saved.getEmail());
        assertEquals("New User", saved.getName());
        assertEquals(6, saved.getToken().length());
        assertNotNull(saved.getExpiresAt());

        verify(emailService).sendRegistrationToken(eq("new@example.com"), eq("New User"), anyString());
    }

    @Test
    void generateRegistrationToken_shouldThrowWhenEmailAlreadyRegistered() {
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(createUser()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.generateRegistrationToken("User", "existing@example.com"));
        assertTrue(ex.getMessage().contains("ja esta registrado"));

        verify(registrationTokenRepository, never()).save(any());
        verify(emailService, never()).sendRegistrationToken(any(), any(), any());
    }

    // confirmRegistration tests

    @Test
    void confirmRegistration_shouldCreateUserAndDeleteToken() {
        RegistrationToken token = new RegistrationToken();
        token.setEmail("new@example.com");
        token.setName("New User");
        token.setToken("123456");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(registrationTokenRepository.findByEmailAndToken("new@example.com", "123456"))
                .thenReturn(Optional.of(token));

        User createdUser = createUser();
        when(createUserUseCase.execute(any(CreateUserDTO.class))).thenReturn(createdUser);

        User result = authService.confirmRegistration("new@example.com", "123456", "mypassword");

        assertEquals(createdUser, result);

        ArgumentCaptor<CreateUserDTO> dtoCaptor = ArgumentCaptor.forClass(CreateUserDTO.class);
        verify(createUserUseCase).execute(dtoCaptor.capture());
        assertEquals("New User", dtoCaptor.getValue().getName());
        assertEquals("new@example.com", dtoCaptor.getValue().getEmail());
        assertEquals("mypassword", dtoCaptor.getValue().getPassword());

        verify(registrationTokenRepository).deleteByEmail("new@example.com");
    }

    @Test
    void confirmRegistration_shouldThrowWhenTokenNotFound() {
        when(registrationTokenRepository.findByEmailAndToken("x@x.com", "000000"))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.confirmRegistration("x@x.com", "000000", "pass"));
        assertTrue(ex.getMessage().contains("invalido"));
    }

    @Test
    void confirmRegistration_shouldThrowAndDeleteWhenTokenExpired() {
        RegistrationToken token = new RegistrationToken();
        token.setEmail("x@x.com");
        token.setToken("123456");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(registrationTokenRepository.findByEmailAndToken("x@x.com", "123456"))
                .thenReturn(Optional.of(token));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.confirmRegistration("x@x.com", "123456", "pass"));
        assertTrue(ex.getMessage().contains("expirado"));

        verify(registrationTokenRepository).delete(token);
        verify(createUserUseCase, never()).execute(any());
    }
}
