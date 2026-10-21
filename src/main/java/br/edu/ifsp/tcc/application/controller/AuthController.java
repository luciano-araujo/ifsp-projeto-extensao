package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.usecase.ConfirmRegistrationUseCase;
import br.edu.ifsp.tcc.application.usecase.LoginUseCase;
import br.edu.ifsp.tcc.application.usecase.RequestRegistrationTokenUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RequestRegistrationTokenUseCase requestRegistrationTokenUseCase;
    private final ConfirmRegistrationUseCase confirmRegistrationUseCase;

    public AuthController(LoginUseCase loginUseCase,
                          RequestRegistrationTokenUseCase requestRegistrationTokenUseCase,
                          ConfirmRegistrationUseCase confirmRegistrationUseCase) {
        this.loginUseCase = loginUseCase;
        this.requestRegistrationTokenUseCase = requestRegistrationTokenUseCase;
        this.confirmRegistrationUseCase = confirmRegistrationUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginUseCase.execute(request));
    }

    @PostMapping("/register/request-token")
    public ResponseEntity<MessageResponse> requestToken(@Valid @RequestBody RegisterTokenRequest request) {
        requestRegistrationTokenUseCase.execute(request);
        return ResponseEntity.ok(new MessageResponse("Codigo de verificacao enviado para " + request.getEmail()));
    }

    @PostMapping("/register/confirm")
    public ResponseEntity<LoginResponse> confirmRegistration(@Valid @RequestBody ConfirmRegistrationRequest request) {
        return ResponseEntity.ok(confirmRegistrationUseCase.execute(request));
    }
}
