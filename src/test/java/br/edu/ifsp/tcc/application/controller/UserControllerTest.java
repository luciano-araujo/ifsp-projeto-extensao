package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private GetUserByIdUseCase getUserByIdUseCase;
    @Mock private DeleteUserUseCase deleteUserUseCase;
    @Mock private UpdateUserUseCase updateUserUseCase;
    @Mock private ChangePasswordUseCase changePasswordUseCase;
    @Mock private RequestEmailChangeUseCase requestEmailChangeUseCase;
    @Mock private ConfirmEmailChangeUseCase confirmEmailChangeUseCase;
    @InjectMocks private UserController controller;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
    }

    @Test
    void updateUser_shouldReturnUpdatedUser() {
        UpdateUserDTO dto = new UpdateUserDTO();
        User updated = new User();
        when(updateUserUseCase.execute(1L, 1L, dto)).thenReturn(updated);

        ResponseEntity<User> response = controller.updateUser(user, 1L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(updated, response.getBody());
    }

    @Test
    void getUserById_shouldReturnUserWhenFound() {
        User found = new User();
        when(getUserByIdUseCase.execute(1L, 1L)).thenReturn(Optional.of(found));

        ResponseEntity<User> response = controller.getUserById(user, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(found, response.getBody());
    }

    @Test
    void getUserById_shouldReturnNotFoundWhenMissing() {
        when(getUserByIdUseCase.execute(2L, 1L)).thenReturn(Optional.empty());

        ResponseEntity<User> response = controller.getUserById(user, 2L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void deleteUser_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.deleteUser(user, 1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteUserUseCase).execute(1L, 1L);
    }

    @Test
    void changePassword_shouldReturnTheFreshToken() {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        TokenResponse token = new TokenResponse("fresh-jwt");
        when(changePasswordUseCase.execute(1L, 1L, dto)).thenReturn(token);

        ResponseEntity<TokenResponse> response = controller.changePassword(user, 1L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(token, response.getBody());
    }

    @Test
    void requestEmailChange_shouldReturnMessage() {
        RequestEmailChangeDTO dto = new RequestEmailChangeDTO();
        MessageResponse message = new MessageResponse("ok");
        when(requestEmailChangeUseCase.execute(1L, 1L, dto)).thenReturn(message);

        ResponseEntity<MessageResponse> response = controller.requestEmailChange(user, 1L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(message, response.getBody());
    }

    @Test
    void confirmEmailChange_shouldReturnUser() {
        ConfirmEmailChangeDTO dto = new ConfirmEmailChangeDTO();
        User updated = new User();
        when(confirmEmailChangeUseCase.execute(1L, 1L, dto)).thenReturn(updated);

        ResponseEntity<User> response = controller.confirmEmailChange(user, 1L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(updated, response.getBody());
    }
}
