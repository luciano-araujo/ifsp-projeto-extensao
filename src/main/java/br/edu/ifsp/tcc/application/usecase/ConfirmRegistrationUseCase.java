package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ConfirmRegistrationRequest;
import br.edu.ifsp.tcc.application.dto.LoginResponse;
import br.edu.ifsp.tcc.application.entity.RegistrationToken;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.JwtService;
import br.edu.ifsp.tcc.application.service.PasswordPolicy;
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
        PasswordPolicy.requireEncodable(request.getPassword());
        String email = request.getEmail();
        RegistrationToken token = registrationTokenService.findLatestByEmail(email)
                .orElseThrow(() -> invalid("Código de verificação inválido."));

        if (token.isExpired()) {
            registrationTokenService.delete(token);
            throw invalid("Código de verificação expirado. Solicite um novo.");
        }

        // The attempt is counted first and atomically, right or wrong: a right code in a parallel burst cannot slip past the cap
        int attempts = registrationTokenService.registerAttempt(token.getId(), MAX_ATTEMPTS);
        if (attempts < 0) {
            registrationTokenService.deleteByEmail(email);
            throw invalid("Muitas tentativas. Solicite um novo código de verificação.");
        }
        if (!token.getToken().equals(request.getToken())) {
            if (attempts >= MAX_ATTEMPTS) {
                registrationTokenService.deleteByEmail(email);
                throw invalid("Muitas tentativas. Solicite um novo código de verificação.");
            }
            throw invalid("Código de verificação inválido.");
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
