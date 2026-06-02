package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class DeleteUserUseCase {

    private final UserService userService;

    public DeleteUserUseCase(UserService userService) {
        this.userService = userService;
    }

    public void execute(Long id, Long authenticatedUserId) {
        if (!id.equals(authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
        userService.deleteById(id);
    }
}