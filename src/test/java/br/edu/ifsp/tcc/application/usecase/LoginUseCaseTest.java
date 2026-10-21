package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.LoginRequest;
import br.edu.ifsp.tcc.application.dto.LoginResponse;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.JwtService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private LoginUseCase useCase;

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setName("Test");
        user.setPassword("encoded-password");
        return user;
    }

    private LoginRequest request(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    @Test
    void execute_shouldReturnTokenAndUserWhenCredentialsAreValid() {
        User user = createUser();
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-password", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        LoginResponse response = useCase.execute(request("test@example.com", "raw-password"));

        assertEquals("jwt-token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("Test", response.getName());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void execute_shouldThrowWhenEmailNotFound() {
        when(userService.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> useCase.execute(request("unknown@example.com", "password")));
        assertTrue(ex.getMessage().contains("Credenciais invalidas"));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void execute_shouldThrowWhenPasswordIsWrong() {
        User user = createUser();
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> useCase.execute(request("test@example.com", "wrong-password")));
        assertTrue(ex.getMessage().contains("Credenciais invalidas"));
        verify(jwtService, never()).generateToken(any());
    }
}
