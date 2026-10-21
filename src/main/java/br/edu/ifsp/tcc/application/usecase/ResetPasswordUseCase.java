package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ResetPasswordRequest;
import br.edu.ifsp.tcc.application.entity.PasswordResetToken;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.PasswordResetTokenService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ResetPasswordUseCase {

    /** Wrong codes allowed before the code is discarded (protects the 6-digit code from guessing). */
    public static final int MAX_ATTEMPTS = 5;

    private final PasswordResetTokenService passwordResetTokenService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public ResetPasswordUseCase(PasswordResetTokenService passwordResetTokenService,
                                UserService userService,
                                PasswordEncoder passwordEncoder) {
        this.passwordResetTokenService = passwordResetTokenService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // The attempt counter must be saved even when the code is wrong, so errors do not roll back
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void execute(ResetPasswordRequest request) {
        String email = request.getEmail().trim();
        PasswordResetToken token = passwordResetTokenService.findLatestByEmail(email)
                .orElseThrow(() -> invalid("Codigo invalido ou expirado. Solicite um novo."));

        if (token.isExpired()) {
            passwordResetTokenService.deleteByEmail(email);
            throw invalid("Codigo invalido ou expirado. Solicite um novo.");
        }

        if (!token.getToken().equals(request.getToken())) {
            int attempts = token.getAttempts() + 1;
            if (attempts >= MAX_ATTEMPTS) {
                passwordResetTokenService.deleteByEmail(email);
                throw invalid("Muitas tentativas. Solicite um novo codigo.");
            }
            token.setAttempts(attempts);
            passwordResetTokenService.save(token);
            throw invalid("Codigo invalido.");
        }

        User user = userService.findByEmail(email)
                .orElseThrow(() -> invalid("Codigo invalido ou expirado. Solicite um novo."));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.update(user);
        passwordResetTokenService.deleteByEmail(email);
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
