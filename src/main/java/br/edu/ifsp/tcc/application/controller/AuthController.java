package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.usecase.ConfirmRegistrationUseCase;
import br.edu.ifsp.tcc.application.usecase.GetInviteByTokenUseCase;
import br.edu.ifsp.tcc.application.usecase.LoginUseCase;
import br.edu.ifsp.tcc.application.usecase.RequestPasswordResetUseCase;
import br.edu.ifsp.tcc.application.usecase.RequestRegistrationTokenUseCase;
import br.edu.ifsp.tcc.application.usecase.ResetPasswordUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RequestRegistrationTokenUseCase requestRegistrationTokenUseCase;
    private final ConfirmRegistrationUseCase confirmRegistrationUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final GetInviteByTokenUseCase getInviteByTokenUseCase;

    public AuthController(LoginUseCase loginUseCase,
                          RequestRegistrationTokenUseCase requestRegistrationTokenUseCase,
                          ConfirmRegistrationUseCase confirmRegistrationUseCase,
                          RequestPasswordResetUseCase requestPasswordResetUseCase,
                          ResetPasswordUseCase resetPasswordUseCase,
                          GetInviteByTokenUseCase getInviteByTokenUseCase) {
        this.loginUseCase = loginUseCase;
        this.requestRegistrationTokenUseCase = requestRegistrationTokenUseCase;
        this.confirmRegistrationUseCase = confirmRegistrationUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.getInviteByTokenUseCase = getInviteByTokenUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginUseCase.execute(request));
    }

    @PostMapping("/register/request-token")
    public ResponseEntity<MessageResponse> requestToken(@Valid @RequestBody RegisterTokenRequest request) {
        requestRegistrationTokenUseCase.execute(request);
        return ResponseEntity.ok(new MessageResponse("Código de verificação enviado para " + request.getEmail()));
    }

    @PostMapping("/register/confirm")
    public ResponseEntity<LoginResponse> confirmRegistration(@Valid @RequestBody ConfirmRegistrationRequest request) {
        return ResponseEntity.ok(confirmRegistrationUseCase.execute(request));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        requestPasswordResetUseCase.execute(request);
        // Same answer for every e-mail, so the route does not reveal which accounts exist
        return ResponseEntity.ok(new MessageResponse("Se o e-mail estiver cadastrado, enviaremos um código de recuperação."));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request);
        return ResponseEntity.ok(new MessageResponse("Senha redefinida com sucesso."));
    }

    // Public: the /convite page shows the invite before the person logs in or signs up
    @GetMapping("/invites/{token}")
    public ResponseEntity<PublicInviteResponse> getInvite(@PathVariable String token) {
        return ResponseEntity.ok(getInviteByTokenUseCase.execute(token));
    }
}
