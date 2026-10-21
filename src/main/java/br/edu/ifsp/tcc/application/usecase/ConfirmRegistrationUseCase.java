package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ConfirmRegistrationRequest;
import br.edu.ifsp.tcc.application.dto.LoginResponse;
import br.edu.ifsp.tcc.application.entity.RegistrationToken;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.JwtService;
import br.edu.ifsp.tcc.application.service.RegistrationTokenService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ConfirmRegistrationUseCase {

    /** Wrong codes allowed before the code is discarded (BE-43: protects the 6-digit code from guessing). */
    public static final int MAX_ATTEMPTS = 5;

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
    // The attempt counter must be saved even when the code is wrong, so errors do not roll back
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public LoginResponse execute(ConfirmRegistrationRequest request) {
        String email = request.getEmail();
        RegistrationToken token = registrationTokenService.findLatestByEmail(email)
                .orElseThrow(() -> invalid("Codigo de verificacao invalido."));

        if (token.isExpired()) {
            registrationTokenService.delete(token);
            throw invalid("Codigo de verificacao expirado. Solicite um novo.");
        }

        if (!token.getToken().equals(request.getToken())) {
            int attempts = token.getAttempts() + 1;
            if (attempts >= MAX_ATTEMPTS) {
                registrationTokenService.deleteByEmail(email);
                throw invalid("Muitas tentativas. Solicite um novo codigo de verificacao.");
            }
            token.setAttempts(attempts);
            registrationTokenService.save(token);
            throw invalid("Codigo de verificacao invalido.");
        }

        User user = new User();
        user.setName(token.getName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User saved = userService.save(user);

        registrationTokenService.deleteByEmail(email);

        return new LoginResponse(jwtService.generateToken(saved), saved.getId(), saved.getName(), saved.getEmail());
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
