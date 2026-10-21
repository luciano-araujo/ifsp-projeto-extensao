package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ForgotPasswordRequest;
import br.edu.ifsp.tcc.application.entity.PasswordResetToken;
import br.edu.ifsp.tcc.application.service.EmailService;
import br.edu.ifsp.tcc.application.service.PasswordResetTokenService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Component
public class RequestPasswordResetUseCase {

    private final UserService userService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.registration.token-expiry-minutes}")
    private int tokenExpiryMinutes;

    public RequestPasswordResetUseCase(UserService userService,
                                       PasswordResetTokenService passwordResetTokenService,
                                       EmailService emailService) {
        this.userService = userService;
        this.passwordResetTokenService = passwordResetTokenService;
        this.emailService = emailService;
    }

    /**
     * Sends a 6-digit reset code when the e-mail belongs to an account. Unknown e-mails are ignored
     * silently, so the response never reveals which e-mails are registered.
     */
    @Transactional
    public void execute(ForgotPasswordRequest request) {
        String email = request.getEmail().trim();
        userService.findByEmail(email).ifPresent(user -> {
            passwordResetTokenService.deleteByEmail(email);

            String tokenCode = String.format("%06d", secureRandom.nextInt(1_000_000));
            PasswordResetToken token = new PasswordResetToken();
            token.setEmail(email);
            token.setToken(tokenCode);
            token.setExpiresAt(LocalDateTime.now().plusMinutes(tokenExpiryMinutes));
            passwordResetTokenService.save(token);

            emailService.sendPasswordResetToken(email, user.getName(), tokenCode);
        });
    }
}
