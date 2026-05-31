package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.AuthService;
import br.edu.ifsp.tcc.application.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.authenticate(request.getEmail(), request.getPassword());
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new LoginResponse(token, user.getId(), user.getName(), user.getEmail()));
    }

    @PostMapping("/register/request-token")
    public ResponseEntity<MessageResponse> requestToken(@Valid @RequestBody RegisterTokenRequest request) {
        authService.generateRegistrationToken(request.getName(), request.getEmail());
        return ResponseEntity.ok(new MessageResponse("Codigo de verificacao enviado para " + request.getEmail()));
    }

    @PostMapping("/register/confirm")
    public ResponseEntity<LoginResponse> confirmRegistration(@Valid @RequestBody ConfirmRegistrationRequest request) {
        User user = authService.confirmRegistration(request.getEmail(), request.getToken(), request.getPassword());
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new LoginResponse(token, user.getId(), user.getName(), user.getEmail()));
    }
}
