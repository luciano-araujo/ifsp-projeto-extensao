package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserByIdUseCaseTest {

    @Mock private UserService userService;
    @InjectMocks private GetUserByIdUseCase getUserByIdUseCase;

    @Test
    void execute_shouldReturnUserWhenFound() {
        User user = new User();
        user.setId(1L);
        when(userService.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = getUserByIdUseCase.execute(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void execute_shouldReturnEmptyWhenNotFound() {
        when(userService.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = getUserByIdUseCase.execute(99L);

        assertTrue(result.isEmpty());
    }
}
