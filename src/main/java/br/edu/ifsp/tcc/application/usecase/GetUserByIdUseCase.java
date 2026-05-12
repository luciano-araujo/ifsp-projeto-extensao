package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.UserService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GetUserByIdUseCase {

    private final UserService userService;

    public GetUserByIdUseCase(UserService userService) {
        this.userService = userService;
    }

    public Optional<User> execute(Long id) {
        return userService.findById(id);
    }
}