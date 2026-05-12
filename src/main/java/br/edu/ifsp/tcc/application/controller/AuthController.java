package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.AuthService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000") // Required to accept requests from Next.js
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = authService.authenticate(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(new AuthResponse("fake-jwt-token-123", user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/register/request-token")
    public ResponseEntity<?> requestToken(@RequestBody TokenRequest request) {
        try {
            authService.generateRegistrationToken(request.getName(), request.getEmail());
            return ResponseEntity.ok().body("{\"message\": \"Token enviado\"}");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/register/confirm")
    public ResponseEntity<?> confirmRegistration(@RequestBody ConfirmRegistrationRequest request) {
        try {
            User user = authService.confirmRegistration(request.getEmail(), request.getToken(), request.getPassword());
            return ResponseEntity.ok(new AuthResponse("fake-jwt-token-123", user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}

@Data class LoginRequest { private String email; private String password; }
@Data class TokenRequest { private String name; private String email; }
@Data class ConfirmRegistrationRequest { private String email; private String token; private String password; }

@Data class AuthResponse {
    private String token;
    private UserDto user;

    public AuthResponse(String token, User user) {
        this.token = token;
        this.user = new UserDto(user.getName(), user.getEmail());
    }
}

@Data class UserDto {
    private String name;
    private String email;

    public UserDto(String name, String email) {
        this.name = name;
        this.email = email;
    }
}