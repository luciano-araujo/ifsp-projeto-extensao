package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.UpdateUserDTO;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.DeleteUserUseCase;
import br.edu.ifsp.tcc.application.usecase.GetUserByIdUseCase;
import br.edu.ifsp.tcc.application.usecase.UpdateUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final GetUserByIdUseCase getUserByIdUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;

    public UserController(GetUserByIdUseCase getUserByIdUseCase,
                          DeleteUserUseCase deleteUserUseCase,
                          UpdateUserUseCase updateUserUseCase) {
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@AuthenticationPrincipal User user,
                                           @PathVariable Long id,
                                           @Valid @RequestBody UpdateUserDTO dto) {
        User updatedUser = updateUserUseCase.execute(id, user.getId(), dto);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@AuthenticationPrincipal User user,
                                            @PathVariable Long id) {
        Optional<User> found = getUserByIdUseCase.execute(id, user.getId());
        return found.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal User user,
                                           @PathVariable Long id) {
        deleteUserUseCase.execute(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
