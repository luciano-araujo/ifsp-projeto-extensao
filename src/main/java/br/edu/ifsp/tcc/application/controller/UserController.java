package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.CreateUserDTO;
import br.edu.ifsp.tcc.application.dto.UpdateUserDTO;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.CreateUserUseCase;
import br.edu.ifsp.tcc.application.usecase.DeleteUserUseCase;
import br.edu.ifsp.tcc.application.usecase.GetUserByIdUseCase;
import br.edu.ifsp.tcc.application.usecase.UpdateUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;

    public UserController(CreateUserUseCase createUserUseCase,
                          GetUserByIdUseCase getUserByIdUseCase,
                          DeleteUserUseCase deleteUserUseCase,
                          UpdateUserUseCase updateUserUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
    }

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserDTO dto) {
        User createdUser = createUserUseCase.execute(dto);
        return ResponseEntity.ok(createdUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserDTO dto) {
        User updatedUser = updateUserUseCase.execute(id, dto);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = getUserByIdUseCase.execute(id);
        return user.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        deleteUserUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
