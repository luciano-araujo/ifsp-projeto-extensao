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
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateUserUseCaseTest {

    @Mock private UserService userService;
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
    void execute_shouldUpdateOnlyTheName() {
        User user = existingUser();
        when(userService.findById(1L)).thenReturn(Optional.of(user));
        when(userService.update(user)).thenReturn(user);

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setName("New Name");

        User result = updateUserUseCase.execute(1L, 1L, dto);

        assertEquals("New Name", result.getName());
        assertEquals("old@example.com", result.getEmail());
    }

    @Test
    void execute_shouldIgnoreBlankFields() {
        User user = existingUser();
        when(userService.findById(1L)).thenReturn(Optional.of(user));
        when(userService.update(user)).thenReturn(user);

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setName("   ");

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
}
