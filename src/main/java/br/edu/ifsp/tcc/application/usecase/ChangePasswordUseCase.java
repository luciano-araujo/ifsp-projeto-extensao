package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ChangePasswordDTO;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.PasswordPolicy;
import br.edu.ifsp.tcc.application.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@Component
public class ChangePasswordUseCase {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public ChangePasswordUseCase(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /** Only the user themself; the current password must match (400 otherwise). */
    @Transactional
    public void execute(Long id, Long authenticatedUserId, ChangePasswordDTO dto) {
        if (!id.equals(authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
        User user = userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        boolean tooLong = dto.getCurrentPassword().getBytes(StandardCharsets.UTF_8).length > PasswordPolicy.MAX_BYTES;
        if (tooLong || !passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta.");
        }
        PasswordPolicy.requireEncodable(dto.getNewPassword());

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userService.update(user);
    }
}
