package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.*;
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
    private final ChangePasswordUseCase changePasswordUseCase;
    private final RequestEmailChangeUseCase requestEmailChangeUseCase;
    private final ConfirmEmailChangeUseCase confirmEmailChangeUseCase;

    public UserController(GetUserByIdUseCase getUserByIdUseCase,
                          DeleteUserUseCase deleteUserUseCase,
                          UpdateUserUseCase updateUserUseCase,
                          ChangePasswordUseCase changePasswordUseCase,
                          RequestEmailChangeUseCase requestEmailChangeUseCase,
                          ConfirmEmailChangeUseCase confirmEmailChangeUseCase) {
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.requestEmailChangeUseCase = requestEmailChangeUseCase;
        this.confirmEmailChangeUseCase = confirmEmailChangeUseCase;
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@AuthenticationPrincipal User user,
                                           @PathVariable Long id,
                                           @Valid @RequestBody UpdateUserDTO dto) {
        User updatedUser = updateUserUseCase.execute(id, user.getId(), dto);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal User user,
                                               @PathVariable Long id,
                                               @Valid @RequestBody ChangePasswordDTO dto) {
        changePasswordUseCase.execute(id, user.getId(), dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/email-change")
    public ResponseEntity<MessageResponse> requestEmailChange(@AuthenticationPrincipal User user,
                                                              @PathVariable Long id,
                                                              @Valid @RequestBody RequestEmailChangeDTO dto) {
        return ResponseEntity.ok(requestEmailChangeUseCase.execute(id, user.getId(), dto));
    }

    @PostMapping("/{id}/email-change/confirm")
    public ResponseEntity<User> confirmEmailChange(@AuthenticationPrincipal User user,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody ConfirmEmailChangeDTO dto) {
        return ResponseEntity.ok(confirmEmailChangeUseCase.execute(id, user.getId(), dto));
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
