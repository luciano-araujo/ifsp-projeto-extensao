package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateUserDTO;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private CreateUserUseCase createUserUseCase;

    @Test
    void execute_shouldEncodePasswordAndSaveUser() {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setName("Test User");
        dto.setEmail("test@example.com");
        dto.setPassword("rawpassword");

        when(passwordEncoder.encode("rawpassword")).thenReturn("encoded");
        when(userService.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        User result = createUserUseCase.execute(dto);

        assertNotNull(result);
        assertEquals("Test User", result.getName());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("encoded", result.getPassword());

        verify(passwordEncoder).encode("rawpassword");
        verify(userService).save(any(User.class));
    }
}
