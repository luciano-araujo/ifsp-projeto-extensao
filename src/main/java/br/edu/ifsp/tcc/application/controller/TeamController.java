package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final ListTeamsUseCase listTeamsUseCase;
    private final CreateTeamUseCase createTeamUseCase;
    private final ListTeamMembersUseCase listTeamMembersUseCase;
    private final AddTeamMemberUseCase addTeamMemberUseCase;
    private final UpdateTeamMemberRoleUseCase updateTeamMemberRoleUseCase;
    private final RemoveTeamMemberUseCase removeTeamMemberUseCase;

    public TeamController(ListTeamsUseCase listTeamsUseCase,
                          CreateTeamUseCase createTeamUseCase,
                          ListTeamMembersUseCase listTeamMembersUseCase,
                          AddTeamMemberUseCase addTeamMemberUseCase,
                          UpdateTeamMemberRoleUseCase updateTeamMemberRoleUseCase,
                          RemoveTeamMemberUseCase removeTeamMemberUseCase) {
        this.listTeamsUseCase = listTeamsUseCase;
        this.createTeamUseCase = createTeamUseCase;
        this.listTeamMembersUseCase = listTeamMembersUseCase;
        this.addTeamMemberUseCase = addTeamMemberUseCase;
        this.updateTeamMemberRoleUseCase = updateTeamMemberRoleUseCase;
        this.removeTeamMemberUseCase = removeTeamMemberUseCase;
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(listTeamsUseCase.execute(user));
    }

    @PostMapping
    public ResponseEntity<TeamResponse> create(@AuthenticationPrincipal User user,
                                               @Valid @RequestBody CreateTeamDTO dto) {
        return ResponseEntity.ok(createTeamUseCase.execute(user, dto));
    }

    // --- Members ---

    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> listMembers(@AuthenticationPrincipal User user,
                                                                @PathVariable Long teamId) {
        return ResponseEntity.ok(listTeamMembersUseCase.execute(teamId, user.getId()));
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamMemberResponse> addMember(@AuthenticationPrincipal User user,
                                                        @PathVariable Long teamId,
                                                        @Valid @RequestBody AddTeamMemberDTO dto) {
        return ResponseEntity.ok(addTeamMemberUseCase.execute(teamId, user.getId(), dto));
    }

    @PatchMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<TeamMemberResponse> updateMember(@AuthenticationPrincipal User user,
                                                           @PathVariable Long teamId,
                                                           @PathVariable Long memberId,
                                                           @Valid @RequestBody UpdateTeamMemberDTO dto) {
        return ResponseEntity.ok(updateTeamMemberRoleUseCase.execute(teamId, user.getId(), memberId, dto));
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@AuthenticationPrincipal User user,
                                             @PathVariable Long teamId,
                                             @PathVariable Long memberId) {
        removeTeamMemberUseCase.execute(teamId, user.getId(), memberId);
        return ResponseEntity.noContent().build();
    }
}
