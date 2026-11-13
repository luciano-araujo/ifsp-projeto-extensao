package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.MessageResponse;
import br.edu.ifsp.tcc.application.dto.RequestEmailChangeDTO;
import br.edu.ifsp.tcc.application.entity.EmailChangeToken;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.EmailChangeTokenService;
import br.edu.ifsp.tcc.application.service.EmailService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Component
public class RequestEmailChangeUseCase {

    private final UserService userService;
    private final EmailChangeTokenService emailChangeTokenService;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.registration.token-expiry-minutes}")
    private int tokenExpiryMinutes;

    public RequestEmailChangeUseCase(UserService userService,
                                     EmailChangeTokenService emailChangeTokenService,
                                     EmailService emailService) {
        this.userService = userService;
        this.emailChangeTokenService = emailChangeTokenService;
        this.emailService = emailService;
    }

    /** Sends a 6-digit code to the new address (replacing any previous request of the user); 409 if it is taken. */
    @Transactional
    public MessageResponse execute(Long id, Long authenticatedUserId, RequestEmailChangeDTO dto) {
        if (!id.equals(authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
        User user = userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        String newEmail = dto.getNewEmail().trim();
        if (newEmail.equalsIgnoreCase(user.getEmail()) || userService.findByEmail(newEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está em uso.");
        }

        emailChangeTokenService.deleteByUserId(id);
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        EmailChangeToken token = new EmailChangeToken();
        token.setUser(user);
        token.setNewEmail(newEmail);
        token.setToken(code);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(tokenExpiryMinutes));
        emailChangeTokenService.save(token);

        emailService.sendEmailChangeToken(newEmail, user.getName(), code);
        return new MessageResponse("Enviamos um código de verificação para " + newEmail + ".");
    }
}
