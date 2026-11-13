package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.usecase.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private LoginUseCase loginUseCase;
    @Mock private RequestRegistrationTokenUseCase requestRegistrationTokenUseCase;
    @Mock private ConfirmRegistrationUseCase confirmRegistrationUseCase;
    @Mock private RequestPasswordResetUseCase requestPasswordResetUseCase;
    @Mock private ResetPasswordUseCase resetPasswordUseCase;
    @Mock private GetInviteByTokenUseCase getInviteByTokenUseCase;
    @InjectMocks private AuthController controller;

    @Test
    void login_shouldReturnLoginResponse() {
        LoginRequest request = new LoginRequest();
        LoginResponse loginResponse = mock(LoginResponse.class);
        when(loginUseCase.execute(request)).thenReturn(loginResponse);

        ResponseEntity<LoginResponse> response = controller.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(loginResponse, response.getBody());
    }

    @Test
    void requestToken_shouldReturnMessageWithEmail() {
        RegisterTokenRequest request = new RegisterTokenRequest();
        request.setEmail("ana@teste.com");

        ResponseEntity<MessageResponse> response = controller.requestToken(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("ana@teste.com"));
        verify(requestRegistrationTokenUseCase).execute(request);
    }

    @Test
    void confirmRegistration_shouldReturnLoginResponse() {
        ConfirmRegistrationRequest request = new ConfirmRegistrationRequest();
        LoginResponse loginResponse = mock(LoginResponse.class);
        when(confirmRegistrationUseCase.execute(request)).thenReturn(loginResponse);

        ResponseEntity<LoginResponse> response = controller.confirmRegistration(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(loginResponse, response.getBody());
    }

    @Test
    void forgotPassword_shouldReturnGenericMessage() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();

        ResponseEntity<MessageResponse> response = controller.forgotPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getMessage());
        verify(requestPasswordResetUseCase).execute(request);
    }

    @Test
    void resetPassword_shouldReturnSuccessMessage() {
        ResetPasswordRequest request = new ResetPasswordRequest();

        ResponseEntity<MessageResponse> response = controller.resetPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getMessage());
        verify(resetPasswordUseCase).execute(request);
    }

    @Test
    void getInvite_shouldReturnPublicInvite() {
        PublicInviteResponse invite = new PublicInviteResponse("Equipe", null, "ana@teste.com", "Bia", null);
        when(getInviteByTokenUseCase.execute("tok")).thenReturn(invite);

        ResponseEntity<PublicInviteResponse> response = controller.getInvite("tok");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(invite, response.getBody());
    }
}
