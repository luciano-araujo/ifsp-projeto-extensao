package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.service.EmailChangeTokenService;
import br.edu.ifsp.tcc.application.service.InviteService;
import br.edu.ifsp.tcc.application.service.PasswordResetTokenService;
import br.edu.ifsp.tcc.application.service.RegistrationTokenService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class PurgeExpiredCodesUseCase {

    private final RegistrationTokenService registrationTokenService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailChangeTokenService emailChangeTokenService;
    private final InviteService inviteService;

    public PurgeExpiredCodesUseCase(RegistrationTokenService registrationTokenService,
                                    PasswordResetTokenService passwordResetTokenService,
                                    EmailChangeTokenService emailChangeTokenService,
                                    InviteService inviteService) {
        this.registrationTokenService = registrationTokenService;
        this.passwordResetTokenService = passwordResetTokenService;
        this.emailChangeTokenService = emailChangeTokenService;
        this.inviteService = inviteService;
    }

    /**
     * Deletes sign-up, password-reset and e-mail-change codes and pending invites that already expired.
     * Codes that are used, discarded after too many attempts or replaced are deleted right away; this removes
     * the ones nobody came back to.
     */
    @Transactional
    public int execute() {
        LocalDateTime now = LocalDateTime.now();
        return registrationTokenService.deleteExpired(now) + passwordResetTokenService.deleteExpired(now)
                + emailChangeTokenService.deleteExpired(now) + inviteService.deleteExpired(now);
    }
}
