package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ConfirmRegistrationRequest;
import br.edu.ifsp.tcc.application.dto.LoginResponse;
import br.edu.ifsp.tcc.application.entity.RegistrationToken;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.JwtService;
import br.edu.ifsp.tcc.application.service.RegistrationTokenService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConfirmRegistrationUseCase {

    private final RegistrationTokenService registrationTokenService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public ConfirmRegistrationUseCase(RegistrationTokenService registrationTokenService,
                                      UserService userService,
                                      PasswordEncoder passwordEncoder,
                                      JwtService jwtService) {
        this.registrationTokenService = registrationTokenService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Validates the e-mail code, creates the account with the encoded password and logs the user in. */
    @Transactional
    public LoginResponse execute(ConfirmRegistrationRequest request) {
        String email = request.getEmail();
        RegistrationToken token = registrationTokenService.findByEmailAndToken(email, request.getToken())
                .orElseThrow(() -> new RuntimeException("Codigo de verificacao invalido."));

        if (token.isExpired()) {
            registrationTokenService.delete(token);
            throw new RuntimeException("Codigo de verificacao expirado. Solicite um novo.");
        }

        User user = new User();
        user.setName(token.getName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User saved = userService.save(user);

        registrationTokenService.deleteByEmail(email);

        return new LoginResponse(jwtService.generateToken(saved), saved.getId(), saved.getName(), saved.getEmail());
    }
}
