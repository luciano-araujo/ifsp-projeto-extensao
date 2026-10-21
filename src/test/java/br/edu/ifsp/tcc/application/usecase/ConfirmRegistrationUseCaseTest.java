package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ConfirmRegistrationRequest;
import br.edu.ifsp.tcc.application.dto.LoginResponse;
import br.edu.ifsp.tcc.application.entity.RegistrationToken;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.JwtService;
import br.edu.ifsp.tcc.application.service.RegistrationTokenService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmRegistrationUseCaseTest {

    @Mock private RegistrationTokenService registrationTokenService;
    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private ConfirmRegistrationUseCase useCase;

    private ConfirmRegistrationRequest request(String email, String token, String password) {
        ConfirmRegistrationRequest request = new ConfirmRegistrationRequest();
        request.setEmail(email);
        request.setToken(token);
        request.setPassword(password);
        return request;
    }

    private RegistrationToken token(String email, LocalDateTime expiresAt) {
        RegistrationToken token = new RegistrationToken();
        token.setEmail(email);
        token.setName("New User");
        token.setToken("123456");
        token.setExpiresAt(expiresAt);
        return token;
    }

    @Test
    void execute_shouldCreateUserWithEncodedPasswordDeleteTokenAndLogIn() {
        when(registrationTokenService.findByEmailAndToken("new@example.com", "123456"))
                .thenReturn(Optional.of(token("new@example.com", LocalDateTime.now().plusMinutes(5))));
        when(passwordEncoder.encode("mypassword")).thenReturn("encoded");
        when(userService.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        LoginResponse response = useCase.execute(request("new@example.com", "123456", "mypassword"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userCaptor.capture());
        assertEquals("New User", userCaptor.getValue().getName());
        assertEquals("new@example.com", userCaptor.getValue().getEmail());
        assertEquals("encoded", userCaptor.getValue().getPassword());

        verify(registrationTokenService).deleteByEmail("new@example.com");
        assertEquals("jwt-token", response.getToken());
        assertEquals(1L, response.getUserId());
    }

    @Test
    void execute_shouldThrowWhenTokenNotFound() {
        when(registrationTokenService.findByEmailAndToken("x@x.com", "000000")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> useCase.execute(request("x@x.com", "000000", "pass")));
        assertTrue(ex.getMessage().contains("invalido"));
        verify(userService, never()).save(any());
    }

    @Test
    void execute_shouldThrowAndDeleteWhenTokenExpired() {
        RegistrationToken expired = token("x@x.com", LocalDateTime.now().minusMinutes(1));
        when(registrationTokenService.findByEmailAndToken("x@x.com", "123456")).thenReturn(Optional.of(expired));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> useCase.execute(request("x@x.com", "123456", "pass")));
        assertTrue(ex.getMessage().contains("expirado"));

        verify(registrationTokenService).delete(expired);
        verify(userService, never()).save(any());
    }
}
