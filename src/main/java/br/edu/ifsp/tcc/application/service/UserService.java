package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CreateUserDTO;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.CreateUserUseCase;
import br.edu.ifsp.tcc.application.usecase.DeleteUserUseCase;
import br.edu.ifsp.tcc.application.usecase.GetUserByIdUseCase;
import org.springframework.data.relational.core.sql.Delete;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final DeleteUserUseCase deleteUserUseCase;

    public UserService(CreateUserUseCase createUserUseCase,
                       GetUserByIdUseCase getUserByIdUseCase,
                       DeleteUserUseCase deleteUserUseCase){
        this.createUserUseCase = createUserUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
    }

    public User createUser(CreateUserDTO dto) {
        return createUserUseCase.execute(
                new CreateUserUseCase.input(dto.name(), dto.email())
        );
    }

    public void deleteUser(Long id) {
        deleteUserUseCase.execute(id);
    }

    public User getUserById(Long id) {
        getUserByIdUseCase.execute(id);
    }
}