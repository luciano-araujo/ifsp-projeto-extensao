package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setName("Test");
        return user;
    }

    @Test
    void save_shouldDelegateToRepository() {
        User user = createUser();
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.save(user);

        assertEquals(user, result);
        verify(userRepository).save(user);
    }

    @Test
    void update_shouldDelegateToRepository() {
        User user = createUser();
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.update(user);

        assertEquals(user, result);
        verify(userRepository).save(user);
    }

    @Test
    void findById_shouldReturnUserWhenExists() {
        User user = createUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = userService.findById(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        List<User> users = List.of(createUser());
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.findAll();

        assertEquals(1, result.size());
    }

    @Test
    void deleteById_shouldDelegateToRepository() {
        userService.deleteById(1L);

        verify(userRepository).deleteById(1L);
    }
}
