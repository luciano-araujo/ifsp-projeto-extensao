package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteUserUseCaseTest {

    @Mock private UserService userService;
    @InjectMocks private DeleteUserUseCase deleteUserUseCase;

    @Test
    void execute_shouldDelegateToService() {
        deleteUserUseCase.execute(1L);
        verify(userService).deleteById(1L);
    }
}
