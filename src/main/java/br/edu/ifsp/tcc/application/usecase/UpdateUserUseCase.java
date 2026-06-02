package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.UpdateUserDTO;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UpdateUserUseCase {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UpdateUserUseCase(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    public User execute(Long id, Long authenticatedUserId, UpdateUserDTO dto) {
        if (!id.equals(authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }

        User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado."));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            user.setName(dto.getName());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        return userService.update(user);
    }
}
