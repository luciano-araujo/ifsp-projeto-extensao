package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.CreateInviteDTO;
import br.edu.ifsp.tcc.application.dto.InviteResponse;
import br.edu.ifsp.tcc.application.dto.TeamResponse;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.AcceptInviteUseCase;
import br.edu.ifsp.tcc.application.usecase.CreateInviteUseCase;
import br.edu.ifsp.tcc.application.usecase.DeleteInviteUseCase;
import br.edu.ifsp.tcc.application.usecase.ListInvitesUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InviteController {

    private final CreateInviteUseCase createInviteUseCase;
    private final ListInvitesUseCase listInvitesUseCase;
    private final DeleteInviteUseCase deleteInviteUseCase;
    private final AcceptInviteUseCase acceptInviteUseCase;

    public InviteController(CreateInviteUseCase createInviteUseCase,
                            ListInvitesUseCase listInvitesUseCase,
                            DeleteInviteUseCase deleteInviteUseCase,
                            AcceptInviteUseCase acceptInviteUseCase) {
        this.createInviteUseCase = createInviteUseCase;
        this.listInvitesUseCase = listInvitesUseCase;
        this.deleteInviteUseCase = deleteInviteUseCase;
        this.acceptInviteUseCase = acceptInviteUseCase;
    }

    @PostMapping("/teams/{teamId}/invites")
    public ResponseEntity<InviteResponse> create(@AuthenticationPrincipal User user,
                                                 @PathVariable Long teamId,
                                                 @Valid @RequestBody CreateInviteDTO dto) {
        return ResponseEntity.ok(createInviteUseCase.execute(teamId, user.getId(), dto));
    }

    @GetMapping("/teams/{teamId}/invites")
    public ResponseEntity<List<InviteResponse>> list(@AuthenticationPrincipal User user,
                                                     @PathVariable Long teamId) {
        return ResponseEntity.ok(listInvitesUseCase.execute(teamId, user.getId()));
    }

    @DeleteMapping("/teams/{teamId}/invites/{inviteId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                       @PathVariable Long teamId,
                                       @PathVariable Long inviteId) {
        deleteInviteUseCase.execute(teamId, user.getId(), inviteId);
        return ResponseEntity.noContent().build();
    }

    // No body: the token travels in the address
    @PostMapping("/invites/{token}/accept")
    public ResponseEntity<TeamResponse> accept(@AuthenticationPrincipal User user,
                                               @PathVariable String token) {
        return ResponseEntity.ok(acceptInviteUseCase.execute(token, user));
    }
}
