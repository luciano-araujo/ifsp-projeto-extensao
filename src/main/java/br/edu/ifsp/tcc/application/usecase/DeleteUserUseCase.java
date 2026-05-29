package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.service.UserService;
import org.springframework.stereotype.Component;

@Component
public class DeleteUserUseCase {

    private final UserService userService;

    public DeleteUserUseCase(UserService userService) {
        this.userService = userService;
    }

    public void execute(Long id) {
        userService.deleteById(id);
    }
}