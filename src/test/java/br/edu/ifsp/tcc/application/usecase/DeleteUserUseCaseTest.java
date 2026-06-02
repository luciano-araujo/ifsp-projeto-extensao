package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteUserUseCaseTest {

    @Mock private UserService userService;
    @InjectMocks private DeleteUserUseCase deleteUserUseCase;

    @Test
    void execute_shouldDelegateToService() {
        deleteUserUseCase.execute(1L, 1L);
        verify(userService).deleteById(1L);
    }

    @Test
    void execute_shouldThrowForbiddenWhenUserIdDoesNotMatch() {
        assertThrows(ResponseStatusException.class, () -> deleteUserUseCase.execute(1L, 2L));
    }
}
