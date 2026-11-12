package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.UpdateUserDTO;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateUserUseCaseTest {

    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UpdateUserUseCase updateUserUseCase;

    private User existingUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Old Name");
        user.setEmail("old@example.com");
        user.setPassword("old-encoded");
        return user;
    }

    @Test
    void execute_shouldUpdateOnlyProvidedFields() {
        User user = existingUser();
        when(userService.findById(1L)).thenReturn(Optional.of(user));
        when(userService.update(user)).thenReturn(user);

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setName("New Name");

        User result = updateUserUseCase.execute(1L, 1L, dto);

        assertEquals("New Name", result.getName());
        assertEquals("old@example.com", result.getEmail());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void execute_shouldEncodePasswordWhenProvided() {
        User user = existingUser();
        when(userService.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("new-encoded");
        when(userService.update(user)).thenReturn(user);

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setPassword("newpass");

        User result = updateUserUseCase.execute(1L, 1L, dto);

        assertEquals("new-encoded", result.getPassword());
        verify(passwordEncoder).encode("newpass");
    }

    @Test
    void execute_shouldIgnoreBlankFields() {
        User user = existingUser();
        when(userService.findById(1L)).thenReturn(Optional.of(user));
        when(userService.update(user)).thenReturn(user);

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setName("   ");
        dto.setEmail("");

        User result = updateUserUseCase.execute(1L, 1L, dto);

        assertEquals("Old Name", result.getName());
        assertEquals("old@example.com", result.getEmail());
    }

    @Test
    void execute_shouldThrowWhenUserNotFound() {
        when(userService.findById(99L)).thenReturn(Optional.empty());

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setName("x");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> updateUserUseCase.execute(99L, 99L, dto));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Usuário não encontrado.", ex.getReason());
    }

    @Test
    void execute_shouldThrowForbiddenWhenUserIdDoesNotMatch() {
        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setName("x");

        assertThrows(ResponseStatusException.class, () -> updateUserUseCase.execute(1L, 2L, dto));
    }

    @Test
    void execute_shouldRejectPasswordOver72Bytes() {
        when(userService.findById(1L)).thenReturn(Optional.of(existingUser()));
        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setPassword("é".repeat(37));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> updateUserUseCase.execute(1L, 1L, dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("A senha deve ter no máximo 72 bytes (caracteres acentuados contam mais).", ex.getReason());
        verifyNoInteractions(passwordEncoder);
        verify(userService, never()).update(any());
    }
}
