package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.service.UserService;

public class UpdateUserUseCase {

    private final UserService userService;

    public UpdateUserUseCase(UserService userService) {
        this.userService = userService;
    }

    public void execute(Long id, String name, String email, String password) {
        userService.updateUser(id, name, email, password);
    }
}
