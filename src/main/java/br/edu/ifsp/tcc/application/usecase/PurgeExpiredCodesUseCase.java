package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.service.PasswordResetTokenService;
import br.edu.ifsp.tcc.application.service.RegistrationTokenService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class PurgeExpiredCodesUseCase {

    private final RegistrationTokenService registrationTokenService;
    private final PasswordResetTokenService passwordResetTokenService;

    public PurgeExpiredCodesUseCase(RegistrationTokenService registrationTokenService,
                                    PasswordResetTokenService passwordResetTokenService) {
        this.registrationTokenService = registrationTokenService;
        this.passwordResetTokenService = passwordResetTokenService;
    }

    /**
     * Deletes sign-up and password-reset codes that already expired. Codes that are used, discarded
     * after too many attempts or replaced are deleted right away; this removes the ones nobody came back to.
     */
    @Transactional
    public int execute() {
        LocalDateTime now = LocalDateTime.now();
        return registrationTokenService.deleteExpired(now) + passwordResetTokenService.deleteExpired(now);
    }
}
