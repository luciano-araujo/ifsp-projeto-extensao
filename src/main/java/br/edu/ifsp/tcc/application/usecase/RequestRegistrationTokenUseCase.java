package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.RegisterTokenRequest;
import br.edu.ifsp.tcc.application.entity.RegistrationToken;
import br.edu.ifsp.tcc.application.service.EmailService;
import br.edu.ifsp.tcc.application.service.RegistrationTokenService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Component
public class RequestRegistrationTokenUseCase {

    private final UserService userService;
    private final RegistrationTokenService registrationTokenService;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.registration.token-expiry-minutes}")
    private int tokenExpiryMinutes;

    public RequestRegistrationTokenUseCase(UserService userService,
                                           RegistrationTokenService registrationTokenService,
                                           EmailService emailService) {
        this.userService = userService;
        this.registrationTokenService = registrationTokenService;
        this.emailService = emailService;
    }

    /** Generates a 6-digit code for the e-mail (replacing any previous one) and sends it. */
    @Transactional
    public void execute(RegisterTokenRequest request) {
        String email = request.getEmail();
        if (userService.findByEmail(email).isPresent()) {
            throw new RuntimeException("Este e-mail ja esta registrado no sistema.");
        }

        registrationTokenService.deleteByEmail(email);

        String tokenCode = String.format("%06d", secureRandom.nextInt(1_000_000));

        RegistrationToken token = new RegistrationToken();
        token.setEmail(email);
        token.setName(request.getName());
        token.setToken(tokenCode);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(tokenExpiryMinutes));

        registrationTokenService.save(token);
        emailService.sendRegistrationToken(email, request.getName(), tokenCode);
    }
}
