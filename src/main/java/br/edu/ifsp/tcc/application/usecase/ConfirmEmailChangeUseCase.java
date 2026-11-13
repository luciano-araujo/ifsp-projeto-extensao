package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ConfirmEmailChangeDTO;
import br.edu.ifsp.tcc.application.entity.EmailChangeToken;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.EmailChangeTokenService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ConfirmEmailChangeUseCase {

    /** Wrong codes allowed before the code is discarded (same rule as the password-reset code). */
    public static final int MAX_ATTEMPTS = 5;

    private final UserService userService;
    private final EmailChangeTokenService emailChangeTokenService;

    public ConfirmEmailChangeUseCase(UserService userService, EmailChangeTokenService emailChangeTokenService) {
        this.userService = userService;
        this.emailChangeTokenService = emailChangeTokenService;
    }

    // The attempt counter must be saved even when the code is wrong, so errors do not roll back
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public User execute(Long id, Long authenticatedUserId, ConfirmEmailChangeDTO dto) {
        if (!id.equals(authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
        User user = userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        EmailChangeToken token = emailChangeTokenService.findByUserId(id)
                .orElseThrow(() -> invalid("Código inválido ou expirado. Solicite um novo."));

        if (token.isExpired()) {
            emailChangeTokenService.deleteByUserId(id);
            throw invalid("Código inválido ou expirado. Solicite um novo.");
        }
        if (!token.getToken().equals(dto.getToken())) {
            int attempts = token.getAttempts() + 1;
            if (attempts >= MAX_ATTEMPTS) {
                emailChangeTokenService.deleteByUserId(id);
                throw invalid("Muitas tentativas. Solicite um novo código.");
            }
            token.setAttempts(attempts);
            emailChangeTokenService.save(token);
            throw invalid("Código inválido.");
        }

        // Someone may have registered the address while the code was pending
        if (userService.findByEmail(token.getNewEmail()).isPresent()) {
            emailChangeTokenService.deleteByUserId(id);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está em uso.");
        }
        user.setEmail(token.getNewEmail());
        User updated = userService.update(user);
        emailChangeTokenService.deleteByUserId(id);
        return updated;
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
